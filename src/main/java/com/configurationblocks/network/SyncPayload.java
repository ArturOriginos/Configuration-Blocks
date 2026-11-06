package com.configurationblocks.network;

import com.configurationblocks.ConfigurationBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public record SyncPayload(ResourceKey<Level> dimension, Map<BlockPos, Entry> entries) implements CustomPacketPayload {
    public record Entry(float hardness, boolean interactive) {}

    public static final Type<SyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ConfigurationBlocks.MODID, "sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeResourceKey(p.dimension());
                buf.writeVarInt(p.entries().size());
                for (Map.Entry<BlockPos, Entry> e : p.entries().entrySet()) {
                    buf.writeBlockPos(e.getKey());
                    buf.writeFloat(e.getValue().hardness());
                    buf.writeBoolean(e.getValue().interactive());
                }
            },
            buf -> {
                ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
                int n = buf.readVarInt();
                Map<BlockPos, Entry> m = new HashMap<>();
                for (int i = 0; i < n; i++) {
                    BlockPos pos = buf.readBlockPos();
                    float h = buf.readFloat();
                    boolean inter = buf.readBoolean();
                    m.put(pos, new Entry(h, inter));
                }
                return new SyncPayload(dim, m);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
