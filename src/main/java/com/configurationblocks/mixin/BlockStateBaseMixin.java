package com.configurationblocks.mixin;

import com.configurationblocks.config.BlockConfig;
import com.configurationblocks.config.ClientConfigCache;
import com.configurationblocks.config.ConfigManager;
import com.configurationblocks.config.LevelConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks that make per-position configuration visible to vanilla code (mining speed, redstone). */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void configurationblocks$destroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (level instanceof ServerLevel sl) {
            LevelConfigs lc = ConfigManager.get(sl);
            if (lc.isEmpty()) return;
            BlockConfig c = lc.get(pos);
            if (c != null && c.hasHardness()) cir.setReturnValue(c.hardness);
        } else if (level instanceof Level l && l.isClientSide) {
            ClientConfigCache.Entry e = ClientConfigCache.get(l, pos);
            if (e != null && e.hasHardness()) cir.setReturnValue(e.hardness());
        }
    }

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void configurationblocks$signal(BlockGetter level, BlockPos pos, Direction dir, CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ServerLevel sl) {
            LevelConfigs lc = ConfigManager.get(sl);
            if (lc.isEmpty()) return;
            BlockConfig c = lc.get(pos);
            if (c != null && c.redstone > 0) cir.setReturnValue(c.redstone);
        }
    }

    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true)
    private void configurationblocks$directSignal(BlockGetter level, BlockPos pos, Direction dir, CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ServerLevel sl) {
            LevelConfigs lc = ConfigManager.get(sl);
            if (lc.isEmpty()) return;
            BlockConfig c = lc.get(pos);
            if (c != null && c.redstone > 0) cir.setReturnValue(c.redstone);
        }
    }
}
