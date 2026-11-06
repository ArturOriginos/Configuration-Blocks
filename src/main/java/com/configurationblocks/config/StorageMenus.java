package com.configurationblocks.config;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Chest / shulker menus whose slots honour the container's configurable stack limit. */
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

    private static int limitFor(Container c, ItemStack stack) {
        int itemMax = stack.getMaxStackSize();
        return itemMax <= 1 ? itemMax : c.getMaxStackSize();
    }

    public static class Chest extends ChestMenu {
        public Chest(int id, Inventory inv, Container container, int rows) {
            super(chestType(rows), id, inv, container, rows);
        }

        @Override
        protected Slot addSlot(Slot slot) {
            if (slot.container instanceof ConfigContainer) {
                slot = new Slot(slot.container, slot.getContainerSlot(), slot.x, slot.y) {
                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return limitFor(this.container, stack);
                    }
                };
            }
            return super.addSlot(slot);
        }
    }

    public static class Shulker extends ShulkerBoxMenu {
        public Shulker(int id, Inventory inv, Container container) {
            super(id, inv, container);
        }

        @Override
        protected Slot addSlot(Slot slot) {
            if (slot instanceof ShulkerBoxSlot && slot.container instanceof ConfigContainer) {
                slot = new ShulkerBoxSlot(slot.container, slot.getContainerSlot(), slot.x, slot.y) {
                    @Override
                    public int getMaxStackSize(ItemStack stack) {
                        return limitFor(this.container, stack);
                    }
                };
            }
            return super.addSlot(slot);
        }
    }
}
