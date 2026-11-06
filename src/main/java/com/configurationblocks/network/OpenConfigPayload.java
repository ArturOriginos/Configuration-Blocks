package com.configurationblocks.network;

import com.configurationblocks.ConfigurationBlocks;
import com.configurationblocks.config.BlockConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenConfigPayload(BlockPos pos, Direction face, BlockConfig config) implements CustomPacketPayload {
    public static final Type<OpenConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ConfigurationBlocks.MODID, "open_config"));

    public static final StreamCodec<FriendlyByteBuf, OpenConfigPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos());
                buf.writeEnum(p.face());
                p.config().write(buf);
            },
            buf -> new OpenConfigPayload(buf.readBlockPos(), buf.readEnum(Direction.class), BlockConfig.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
