package com.configurationblocks.network;

import com.configurationblocks.ConfigurationBlocks;
import com.configurationblocks.config.BlockConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ApplyConfigPayload(BlockPos pos, BlockConfig config) implements CustomPacketPayload {
    public static final Type<ApplyConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ConfigurationBlocks.MODID, "apply_config"));

    public static final StreamCodec<FriendlyByteBuf, ApplyConfigPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos());
                p.config().write(buf);
            },
            buf -> new ApplyConfigPayload(buf.readBlockPos(), BlockConfig.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
