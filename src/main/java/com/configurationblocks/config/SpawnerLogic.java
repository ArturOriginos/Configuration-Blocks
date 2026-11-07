package com.configurationblocks.config;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;

/** Runs a vanilla BaseSpawner for a configured block (no spawner block entity needed). */
public final class SpawnerLogic {
    private SpawnerLogic() {}

    public static void tick(ServerLevel level, BlockPos pos, BlockConfig cfg) {
        if (cfg.spawner == null) {
            BaseSpawner s = new BaseSpawner() {
                @Override
                public void broadcastEvent(Level l, BlockPos p, int event) {
                    // no spawner block entity to notify
                }
            };
            CompoundTag t = new CompoundTag();
            t.putShort("Delay", (short) 20);
            t.putShort("MinSpawnDelay", (short) cfg.spawnerMinDelay);
            t.putShort("MaxSpawnDelay", (short) cfg.spawnerMaxDelay);
            t.putShort("SpawnCount", (short) cfg.spawnerCount);
            t.putShort("MaxNearbyEntities", (short) cfg.spawnerMaxNearby);
            t.putShort("RequiredPlayerRange", (short) cfg.spawnerPlayerRange);
            t.putShort("SpawnRange", (short) cfg.spawnerRange);
            CompoundTag entity = new CompoundTag();
            entity.putString("id", cfg.spawnerEntity.toString());
            CompoundTag data = new CompoundTag();
            data.put("entity", entity);
            t.put("SpawnData", data);
            s.load(level, pos, t);
            cfg.spawner = s;
        }
        cfg.spawner.serverTick(level, pos);
    }
}
