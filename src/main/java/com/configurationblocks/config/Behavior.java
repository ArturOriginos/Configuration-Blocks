package com.configurationblocks.config;

public enum Behavior {
    NONE("none"),
    CHEST("chest"),
    SHULKER_BOX("shulker_box"),
    ENDER_CHEST("ender_chest"),
    CRAFTING_TABLE("crafting_table"),
    FURNACE("furnace"),
    BLAST_FURNACE("blast_furnace"),
    SMOKER("smoker"),
    ANVIL("anvil"),
    ENCHANTING_TABLE("enchanting_table"),
    SMITHING_TABLE("smithing_table"),
    GRINDSTONE("grindstone"),
    LOOM("loom"),
    CARTOGRAPHY_TABLE("cartography_table"),
    STONECUTTER("stonecutter"),
    JUKEBOX("jukebox"),
    SPAWNER("spawner");

    public final String id;

    Behavior(String id) {
        this.id = id;
    }

    public boolean isFurnace() {
        return this == FURNACE || this == BLAST_FURNACE || this == SMOKER;
    }

    public boolean isInteractive() {
        return this != NONE && this != SPAWNER;
    }

    public int containerSize() {
        if (this == CHEST || this == SHULKER_BOX) return 27;
        if (isFurnace()) return 3;
        return 0;
    }

    public String langKey() {
        return "configurationblocks.behavior." + id;
    }

    public static Behavior byOrdinal(int i) {
        Behavior[] v = values();
        return i >= 0 && i < v.length ? v[i] : NONE;
    }

    public static Behavior byId(String id) {
        for (Behavior b : values()) {
            if (b.id.equals(id)) return b;
        }
        return NONE;
    }
}
