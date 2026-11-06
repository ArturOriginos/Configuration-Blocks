package com.configurationblocks;

import com.configurationblocks.network.Network;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(ConfigurationBlocks.MODID)
public class ConfigurationBlocks {
    public static final String MODID = "configurationblocks";

    public ConfigurationBlocks(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        modEventBus.addListener(Network::register);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CONFIGURATOR.get());
        }
    }
}
