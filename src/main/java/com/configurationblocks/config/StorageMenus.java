package com.configurationblocks.config;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** Chest / shulker menus for configured blocks. */
public final class StorageMenus {
    private StorageMenus() {}

    public static MenuType<ChestMenu> chestType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    public static class Chest extends ChestMenu {
        public Chest(int id, Inventory inv, Container container, int rows) {
            super(chestType(rows), id, inv, container, rows);
        }
    }

    public static class Shulker extends ShulkerBoxMenu {
        public Shulker(int id, Inventory inv, Container container) {
            super(id, inv, container);
        }
    }
}
