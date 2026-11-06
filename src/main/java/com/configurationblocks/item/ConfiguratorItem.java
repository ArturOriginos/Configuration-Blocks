package com.configurationblocks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The logic lives in GameEvents (right-click handler); the item itself is just a marker. */
public class ConfiguratorItem extends Item {
    public ConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.configurationblocks.configurator.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.configurationblocks.configurator.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
