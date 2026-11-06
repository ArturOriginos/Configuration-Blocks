package com.configurationblocks.event;

import com.configurationblocks.ConfigurationBlocks;
import com.configurationblocks.ModItems;
import com.configurationblocks.config.*;
import com.configurationblocks.network.OpenConfigPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ConfigurationBlocks.MODID)
public class GameEvents {

    public static boolean holdsConfigurator(Player p) {
        return p.getMainHandItem().is(ModItems.CONFIGURATOR.get())
                || p.getOffhandItem().is(ModItems.CONFIGURATOR.get());
    }

    // ------------------------------------------------------------------ interaction

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        // 1) Configurator in hand: open GUI / reset
        if (holdsConfigurator(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
                Direction face = event.getFace() != null ? event.getFace() : Direction.UP;
                useConfigurator(sp, sl, pos, face);
            }
            return;
        }

        // 2) Configured block functions (chest, furnace, ...)
        boolean hasItem = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        if (player.isSecondaryUseActive() && hasItem) return;

        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            BlockConfig cfg = ConfigManager.get(sl).get(pos);
            if (cfg != null && cfg.behavior.isInteractive()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                BehaviorHandler.interact(sp, sl, pos, cfg, event.getHand());
            }
        } else if (level.isClientSide) {
            ClientConfigCache.Entry e = ClientConfigCache.get(level, pos);
            if (e != null && e.interactive()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    private static void useConfigurator(ServerPlayer sp, ServerLevel level, BlockPos pos, Direction face) {
        if (level.getBlockState(pos).isAir()) return;
        LevelConfigs lc = ConfigManager.get(level);
        BlockConfig existing = lc.get(pos);
        if (existing != null && !existing.canAccess(sp)) {
            sp.displayClientMessage(Component.translatable("message.configurationblocks.locked"), true);
            return;
        }
        if (sp.isShiftKeyDown()) {
            BlockConfig old = lc.remove(pos);
            if (old != null) {
                if (old.behavior == Behavior.JUKEBOX && !old.disc.isEmpty()) level.levelEvent(1011, pos, 0);
                old.dropContents(level, pos.above());
                ConfigManager.applyChange(level, pos);
            }
            sp.displayClientMessage(Component.translatable("message.configurationblocks.cleared"), true);
        } else {
            BlockConfig cfg = lc.get(pos);
            PacketDistributor.sendToPlayer(sp, new OpenConfigPayload(pos.immutable(), face, cfg != null ? cfg : new BlockConfig()));
        }
    }

    // ------------------------------------------------------------------ breaking / explosions

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer().level() instanceof ServerLevel sl)) return;
        LevelConfigs lc = ConfigManager.get(sl);
        if (lc.isEmpty()) return;
        BlockConfig c = lc.get(event.getPos());
        if (c != null && c.hardness == -1.0F && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
        if (c != null && !c.canAccess(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        LevelConfigs lc = ConfigManager.get(sl);
        if (lc.isEmpty()) return;
        event.getAffectedBlocks().removeIf(p -> {
            BlockConfig c = lc.get(p);
            if (c == null || c.blastResistance < 0) return false;
            if (c.blastResistance >= 17.0F) return true;
            return sl.random.nextFloat() < c.blastResistance / 17.0F;
        });
    }

    // ------------------------------------------------------------------ bounce

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity en = event.getEntity();
        if (!(en.level() instanceof ServerLevel sl)) return;
        LevelConfigs lc = ConfigManager.get(sl);
        if (lc.isEmpty()) return;
        BlockPos below = BlockPos.containing(en.getX(), en.getY() - 0.2, en.getZ());
        BlockConfig c = lc.get(below);
        if (c == null || c.bounce <= 0 || en.isShiftKeyDown()) return;
        event.setDamageMultiplier(0.0F);
        if (event.getDistance() > 1.0F) {
            double vy = Math.min(2.5, 0.3 * c.bounce * Math.sqrt(event.getDistance()));
            Vec3 m = en.getDeltaMovement();
            en.setDeltaMovement(m.x, vy, m.z);
            en.hurtMarked = true;
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            ConfigManager.get(sl).tick(sl);
        }
    }

    /** When a gravity block lands, move its configuration to the landing position. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof FallingBlockEntity fb) || !(fb.level() instanceof ServerLevel sl)) return;
        CompoundTag data = fb.getPersistentData();
        if (!data.contains(LevelConfigs.FALLING_KEY)) return;
        if (fb.getRemovalReason() != Entity.RemovalReason.DISCARDED) return;

        CompoundTag tag = data.getCompound(LevelConfigs.FALLING_KEY);
        data.remove(LevelConfigs.FALLING_KEY);
        BlockConfig cfg = BlockConfig.load(tag, sl.registryAccess());
        BlockPos pos = fb.blockPosition();
        BlockState st = fb.getBlockState();
        if (sl.getBlockState(pos).is(st.getBlock())) {
            ConfigManager.get(sl).put(pos, cfg);
            ConfigManager.applyChange(sl, pos);
        } else {
            cfg.dropContents(sl, pos);
        }
    }

    // ------------------------------------------------------------------ client sync

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ConfigManager.syncPlayer(sp);
    }

    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ConfigManager.syncPlayer(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) ConfigManager.syncPlayer(sp);
    }
}
