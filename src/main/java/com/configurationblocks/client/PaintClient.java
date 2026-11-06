package com.configurationblocks.client;

import com.configurationblocks.ConfigurationBlocks;
import com.configurationblocks.network.PaintSyncPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Client side: stores painted faces as dynamic textures and draws them over the blocks. */
@EventBusSubscriber(modid = ConfigurationBlocks.MODID, value = Dist.CLIENT)
public final class PaintClient {
    private static final class Face {
        final int[] pixels;
        final ResourceLocation location;
        final DynamicTexture texture;

        Face(int[] pixels, ResourceLocation location, DynamicTexture texture) {
            this.pixels = pixels;
            this.location = location;
            this.texture = texture;
        }
    }

    private static ResourceKey<Level> dimension;
    private static final Map<BlockPos, EnumMap<Direction, Face>> FACES = new HashMap<>();
    private static int counter;

    private static final double OFFSET = 0.002;
    private static final double MAX_DIST_SQR = 128.0 * 128.0;

    // origin (top-left seen from outside), U (right), V (down) per Direction ordinal
    private static final float[][] AXES = {
            /* DOWN  */ {0, 0, 1, 1, 0, 0, 0, 0, -1},
            /* UP    */ {0, 1, 0, 1, 0, 0, 0, 0, 1},
            /* NORTH */ {1, 1, 0, -1, 0, 0, 0, -1, 0},
            /* SOUTH */ {0, 1, 1, 1, 0, 0, 0, -1, 0},
            /* WEST  */ {0, 1, 0, 0, 0, 1, 0, -1, 0},
            /* EAST  */ {1, 1, 1, 0, 0, -1, 0, -1, 0},
    };

    private PaintClient() {}

    // ------------------------------------------------------------ data

    public static void apply(PaintSyncPayload p) {
        if (p.full() || dimension == null || !dimension.equals(p.dimension())) clear();
        dimension = p.dimension();
        for (Map.Entry<BlockPos, Map<Direction, int[]>> e : p.entries().entrySet()) {
            removePos(e.getKey());
            if (e.getValue().isEmpty()) continue;
            EnumMap<Direction, Face> m = new EnumMap<>(Direction.class);
            for (Map.Entry<Direction, int[]> f : e.getValue().entrySet()) {
                m.put(f.getKey(), createFace(f.getValue()));
            }
            FACES.put(e.getKey(), m);
        }
    }

    public static EnumMap<Direction, int[]> snapshot(BlockPos pos) {
        EnumMap<Direction, int[]> out = new EnumMap<>(Direction.class);
        EnumMap<Direction, Face> m = FACES.get(pos);
        if (m != null) m.forEach((d, f) -> out.put(d, f.pixels.clone()));
        return out;
    }

    public static void clear() {
        for (BlockPos pos : new java.util.ArrayList<>(FACES.keySet())) removePos(pos);
        FACES.clear();
    }

    private static void removePos(BlockPos pos) {
        EnumMap<Direction, Face> old = FACES.remove(pos);
        if (old == null) return;
        for (Face f : old.values()) {
            Minecraft.getInstance().getTextureManager().release(f.location);
        }
    }

    private static int toAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }

    private static Face createFace(int[] px) {
        DynamicTexture tex = new DynamicTexture(16, 16, false);
        NativeImage img = tex.getPixels();
        if (img != null) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    img.setPixelRGBA(x, y, toAbgr(px[y * 16 + x]));
                }
            }
        }
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(ConfigurationBlocks.MODID, "paint/" + (counter++));
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        tex.setFilter(false, false);
        tex.upload();
        return new Face(px.clone(), loc, tex);
    }

    // ------------------------------------------------------------ events

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        dimension = null;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (FACES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !level.dimension().equals(dimension)) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Map.Entry<BlockPos, EnumMap<Direction, Face>> e : FACES.entrySet()) {
            BlockPos pos = e.getKey();
            if (pos.distToCenterSqr(cam.x, cam.y, cam.z) > MAX_DIST_SQR) continue;
            if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir()) continue;

            for (Map.Entry<Direction, Face> f : e.getValue().entrySet()) {
                Direction d = f.getKey();
                BlockPos n = pos.relative(d);
                if (level.getBlockState(n).isSolidRender(level, n)) continue;

                RenderType rt = RenderType.entityTranslucentCull(f.getValue().location);
                VertexConsumer vc = buffers.getBuffer(rt);
                ps.pushPose();
                ps.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
                drawFace(vc, ps.last(), d, LevelRenderer.getLightColor(level, n));
                ps.popPose();
                buffers.endBatch(rt);
            }
        }
    }

    private static void drawFace(VertexConsumer vc, PoseStack.Pose pose, Direction d, int light) {
        float[] a = AXES[d.ordinal()];
        float nx = d.getStepX(), ny = d.getStepY(), nz = d.getStepZ();
        float ox = a[0] + nx * (float) OFFSET, oy = a[1] + ny * (float) OFFSET, oz = a[2] + nz * (float) OFFSET;
        float[][] corners = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (float[] c : corners) {
            float u = c[0], v = c[1];
            float x = ox + a[3] * u + a[6] * v;
            float y = oy + a[4] * u + a[7] * v;
            float z = oz + a[5] * u + a[8] * v;
            vc.addVertex(pose, x, y, z)
                    .setColor(255, 255, 255, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
