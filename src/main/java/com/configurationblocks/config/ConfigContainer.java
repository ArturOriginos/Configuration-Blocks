package com.configurationblocks.config;

import net.minecraft.world.SimpleContainer;

/** SimpleContainer with a configurable per-slot item limit. */
public class ConfigContainer extends SimpleContainer {
    private int limit = 64;

    public ConfigContainer(int size) {
        super(size);
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public int getMaxStackSize() {
        return limit;
    }
}
