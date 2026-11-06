package com.configurationblocks.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;

/** Runs a vanilla BaseSpawner for a configured block (no spawner block entity needed). */
public final class SpawnerLogic {
    private SpawnerLogic() {}

    public static void tick(ServerLevel level, BlockPos pos, BlockConfig cfg) {
        ResourceLocation id = cfg.spawnerEntity;
        if (cfg.spawner == null || !id.equals(cfg.spawnerBound)) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(EntityType.PIG);
            BaseSpawner s = new BaseSpawner() {
                @Override
                public void broadcastEvent(Level l, BlockPos p, int event) {
                    // no spawner block entity to notify
                }
            };
            s.setEntityId(type, level, level.getRandom(), pos);
            cfg.spawner = s;
            cfg.spawnerBound = id;
        }
        cfg.spawner.serverTick(level, pos);
    }
}
