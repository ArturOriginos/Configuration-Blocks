package com.configurationblocks.network;

import com.configurationblocks.ConfigurationBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server: replace the 16x16 pixels of one face. */
public record PaintPayload(BlockPos pos, Direction face, int[] pixels) implements CustomPacketPayload {
    public static final Type<PaintPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ConfigurationBlocks.MODID, "paint"));

    public static final StreamCodec<FriendlyByteBuf, PaintPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos());
                buf.writeEnum(p.face());
                buf.writeVarIntArray(p.pixels());
            },
            buf -> new PaintPayload(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readVarIntArray(256)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
