package com.configurationblocks.network;

import com.configurationblocks.ConfigurationBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Server -> client: painted faces. An empty face map for a position means "remove painting". */
public record PaintSyncPayload(ResourceKey<Level> dimension, boolean full,
                               Map<BlockPos, Map<Direction, int[]>> entries) implements CustomPacketPayload {
    public static final Type<PaintSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ConfigurationBlocks.MODID, "paint_sync"));

    public static final StreamCodec<FriendlyByteBuf, PaintSyncPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeResourceKey(p.dimension());
                buf.writeBoolean(p.full());
                buf.writeVarInt(p.entries().size());
                for (Map.Entry<BlockPos, Map<Direction, int[]>> e : p.entries().entrySet()) {
                    buf.writeBlockPos(e.getKey());
                    buf.writeVarInt(e.getValue().size());
                    for (Map.Entry<Direction, int[]> f : e.getValue().entrySet()) {
                        buf.writeEnum(f.getKey());
                        buf.writeVarIntArray(f.getValue());
                    }
                }
            },
            buf -> {
                ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
                boolean full = buf.readBoolean();
                int n = buf.readVarInt();
                Map<BlockPos, Map<Direction, int[]>> m = new HashMap<>();
                for (int i = 0; i < n; i++) {
                    BlockPos pos = buf.readBlockPos();
                    int faces = buf.readVarInt();
                    Map<Direction, int[]> fm = new EnumMap<>(Direction.class);
                    for (int j = 0; j < faces; j++) {
                        Direction d = buf.readEnum(Direction.class);
                        fm.put(d, buf.readVarIntArray(256));
                    }
                    m.put(pos, fm);
                }
                return new PaintSyncPayload(dim, full, m);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
