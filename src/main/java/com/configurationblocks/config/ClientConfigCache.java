package com.configurationblocks.config;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Map;

/** Client-side copy of the data the client needs (mining speed and interaction prediction). */
public final class ClientConfigCache {
    public record Entry(float hardness, boolean interactive) {
        public boolean hasHardness() {
            return hardness > -1.5F;
        }
    }

    private static volatile ResourceKey<Level> dimension;
    private static volatile Map<BlockPos, Entry> map = Map.of();

    private ClientConfigCache() {}

    public static void set(ResourceKey<Level> dim, Map<BlockPos, Entry> m) {
        dimension = dim;
        map = m;
    }

    public static Entry get(Level level, BlockPos pos) {
        Map<BlockPos, Entry> m = map;
        if (m.isEmpty() || !level.dimension().equals(dimension)) return null;
        return m.get(pos);
    }
}
