package com.configurationblocks.network;

import com.configurationblocks.config.*;
import com.configurationblocks.event.GameEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerHandler {
    private ServerHandler() {}

    public static void apply(ApplyConfigPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();
        BlockPos pos = p.pos();
        if (!GameEvents.holdsConfigurator(sp)) return;
        if (sp.distanceToSqr(Vec3.atCenterOf(pos)) > 144.0) return;
        if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir()) return;

        BlockConfig in = p.config();
        in.sanitize();

        LevelConfigs lc = ConfigManager.get(level);
        BlockConfig cfg = lc.get(pos);
        if (cfg == null) cfg = new BlockConfig();
        if (!cfg.canAccess(sp)) {
            sp.displayClientMessage(Component.translatable("message.configurationblocks.locked"), true);
            return;
        }

        if (cfg.behavior != in.behavior) {
            if (cfg.behavior == Behavior.JUKEBOX && !cfg.disc.isEmpty()) level.levelEvent(1011, pos, 0);
            cfg.dropContents(level, pos.above());
            cfg.resetState();
        }
        cfg.copyBasicFrom(in);
        if (cfg.locked) {
            if (cfg.owner == null) cfg.owner = sp.getUUID();
        } else {
            cfg.owner = null;
        }
        cfg.blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        cfg.fitContainer(level, pos.above());
        cfg.ensureContainer();

        if (cfg.isDefault()) {
            lc.remove(pos);
        } else {
            lc.put(pos, cfg);
        }
        ConfigManager.applyChange(level, pos);
        sp.displayClientMessage(Component.translatable("message.configurationblocks.applied"), true);
    }

    public static void paint(PaintPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();
        BlockPos pos = p.pos();
        if (!GameEvents.holdsConfigurator(sp)) return;
        if (sp.distanceToSqr(Vec3.atCenterOf(pos)) > 144.0) return;
        if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir()) return;
        int[] in = p.pixels();
        if (in.length != 256) return;

        int[] clean = new int[256];
        boolean empty = true;
        for (int i = 0; i < 256; i++) {
            clean[i] = in[i] == 0 ? 0 : (in[i] | 0xFF000000);
            if (clean[i] != 0) empty = false;
        }

        LevelConfigs lc = ConfigManager.get(level);
        BlockConfig cfg = lc.get(pos);
        if (cfg == null) cfg = new BlockConfig();
        if (!cfg.canAccess(sp)) {
            sp.displayClientMessage(Component.translatable("message.configurationblocks.locked"), true);
            return;
        }
        if (cfg.blockId == null) cfg.blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (empty) cfg.paint.remove(p.face());
        else cfg.paint.put(p.face(), clean);

        if (cfg.isDefault()) lc.remove(pos);
        else lc.put(pos, cfg);
        ConfigManager.syncPaint(level, pos);
    }
}
