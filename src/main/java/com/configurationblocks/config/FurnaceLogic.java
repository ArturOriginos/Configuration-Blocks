package com.configurationblocks.config;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Optional;

/** Simplified furnace simulation for configured blocks (no real block entity needed). */
public final class FurnaceLogic {
    private static final int LIT = 0, DURATION = 1, PROGRESS = 2, TOTAL = 3;

    private FurnaceLogic() {}

    private static RecipeType<? extends AbstractCookingRecipe> typeFor(Behavior b) {
        return switch (b) {
            case BLAST_FURNACE -> RecipeType.BLASTING;
            case SMOKER -> RecipeType.SMOKING;
            default -> RecipeType.SMELTING;
        };
    }

    private static Optional<RecipeHolder<? extends AbstractCookingRecipe>> find(ServerLevel level, Behavior b, ItemStack stack) {
        SingleRecipeInput in = new SingleRecipeInput(stack);
        RecipeManager rm = level.getRecipeManager();
        return switch (b) {
            case BLAST_FURNACE -> rm.getRecipeFor(RecipeType.BLASTING, in, level)
                    .map(h -> (RecipeHolder<? extends AbstractCookingRecipe>) h);
            case SMOKER -> rm.getRecipeFor(RecipeType.SMOKING, in, level)
                    .map(h -> (RecipeHolder<? extends AbstractCookingRecipe>) h);
            default -> rm.getRecipeFor(RecipeType.SMELTING, in, level)
                    .map(h -> (RecipeHolder<? extends AbstractCookingRecipe>) h);
        };
    }

    private static boolean canBurn(ServerLevel level, AbstractCookingRecipe recipe, SimpleContainer inv) {
        ItemStack result = recipe.assemble(new SingleRecipeInput(inv.getItem(0)), level.registryAccess());
        if (result.isEmpty()) return false;
        ItemStack out = inv.getItem(2);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, result)) return false;
        return out.getCount() + result.getCount() <= Math.min(inv.getMaxStackSize(), out.getMaxStackSize());
    }

    public static void tick(ServerLevel level, BlockConfig cfg) {
        SimpleContainer inv = cfg.container;
        if (inv == null) return;
        SimpleContainerData d = cfg.furnaceData;
        boolean changed = false;

        if (d.get(LIT) > 0) {
            d.set(LIT, d.get(LIT) - 1);
            changed = true;
        }

        ItemStack input = inv.getItem(0);
        ItemStack fuel = inv.getItem(1);
        boolean hasInput = !input.isEmpty();
        boolean hasFuel = !fuel.isEmpty();

        if (d.get(LIT) > 0 || (hasInput && hasFuel)) {
            Optional<RecipeHolder<? extends AbstractCookingRecipe>> holder =
                    hasInput ? find(level, cfg.behavior, input) : Optional.empty();
            boolean can = holder.isPresent() && canBurn(level, holder.get().value(), inv);

            if (d.get(LIT) <= 0 && can) {
                int burn = fuel.getBurnTime(typeFor(cfg.behavior));
                if (burn > 0) {
                    d.set(LIT, burn);
                    d.set(DURATION, burn);
                    if (fuel.hasCraftingRemainingItem()) {
                        inv.setItem(1, fuel.getCraftingRemainingItem());
                    } else {
                        fuel.shrink(1);
                    }
                    changed = true;
                }
            }

            if (d.get(LIT) > 0 && can) {
                AbstractCookingRecipe recipe = holder.get().value();
                d.set(TOTAL, recipe.getCookingTime());
                int p = d.get(PROGRESS) + 1;
                if (p >= d.get(TOTAL)) {
                    d.set(PROGRESS, 0);
                    ItemStack result = recipe.assemble(new SingleRecipeInput(input), level.registryAccess());
                    ItemStack out = inv.getItem(2);
                    if (out.isEmpty()) inv.setItem(2, result.copy());
                    else out.grow(result.getCount());
                    input.shrink(1);
                } else {
                    d.set(PROGRESS, p);
                }
                changed = true;
            } else if (d.get(PROGRESS) != 0) {
                d.set(PROGRESS, 0);
                changed = true;
            }
        } else if (d.get(PROGRESS) > 0) {
            d.set(PROGRESS, Math.max(0, d.get(PROGRESS) - 2));
            changed = true;
        }

        if (changed) inv.setChanged();
    }
}
