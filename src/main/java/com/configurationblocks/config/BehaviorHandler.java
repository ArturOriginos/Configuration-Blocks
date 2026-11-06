package com.configurationblocks.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/** Opens the GUI / runs the function of a configured block. */
public final class BehaviorHandler {
    private BehaviorHandler() {}

    public static void interact(ServerPlayer player, ServerLevel level, BlockPos pos, BlockConfig cfg, InteractionHand hand) {
        if (!cfg.canAccess(player)) {
            player.displayClientMessage(Component.translatable("message.configurationblocks.locked"), true);
            return;
        }
        ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
        switch (cfg.behavior) {
            case CHEST -> {
                SimpleContainer c = cfg.ensureContainer();
                int rows = cfg.storageRows;
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StorageMenus.Chest(id, inv, c, rows),
                        Component.translatable("container.chest")));
            }
            case SHULKER_BOX -> {
                SimpleContainer c = cfg.ensureContainer();
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StorageMenus.Shulker(id, inv, c),
                        Component.translatable("container.shulkerBox")));
            }
            case ENDER_CHEST -> {
                PlayerEnderChestContainer ec = player.getEnderChestInventory();
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, ec),
                        Component.translatable("container.enderchest")));
            }
            case FURNACE -> {
                SimpleContainer c = cfg.ensureContainer();
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new FurnaceMenu(id, inv, c, cfg.furnaceData),
                        Component.translatable("container.furnace")));
            }
            case BLAST_FURNACE -> {
                SimpleContainer c = cfg.ensureContainer();
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BlastFurnaceMenu(id, inv, c, cfg.furnaceData),
                        Component.translatable("container.blast_furnace")));
            }
            case SMOKER -> {
                SimpleContainer c = cfg.ensureContainer();
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SmokerMenu(id, inv, c, cfg.furnaceData),
                        Component.translatable("container.smoker")));
            }
            case CRAFTING_TABLE -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.crafting")));
            case ANVIL -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AnvilMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.repair")));
            case ENCHANTING_TABLE -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EnchantmentMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.enchant")));
            case SMITHING_TABLE -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SmithingMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.upgrade")));
            case GRINDSTONE -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GrindstoneMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.grindstone_title")));
            case LOOM -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new LoomMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.loom")));
            case CARTOGRAPHY_TABLE -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CartographyTableMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.cartography_table")));
            case STONECUTTER -> player.openMenu(new SimpleMenuProvider((id, inv, p) -> new StonecutterMenu(id, inv, access) {
                @Override public boolean stillValid(Player pl) { return true; }
            }, Component.translatable("container.stonecutter")));
            case JUKEBOX -> jukebox(player, level, pos, cfg, hand);
            default -> {}
        }
    }

    private static void jukebox(ServerPlayer player, ServerLevel level, BlockPos pos, BlockConfig cfg, InteractionHand hand) {
        if (!cfg.disc.isEmpty()) {
            level.levelEvent(1011, pos, 0);
            Block.popResource(level, pos.above(), cfg.disc);
            cfg.disc = ItemStack.EMPTY;
        } else {
            ItemStack held = player.getItemInHand(hand);
            Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(level.registryAccess(), held);
            if (song.isPresent()) {
                cfg.disc = held.copyWithCount(1);
                held.consume(1, player);
                int songId = level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getId(song.get().value());
                level.levelEvent(1010, pos, songId);
            }
        }
        level.getDataStorage().computeIfAbsent(LevelConfigs.factory(), LevelConfigs.NAME).setDirty();
    }
}
