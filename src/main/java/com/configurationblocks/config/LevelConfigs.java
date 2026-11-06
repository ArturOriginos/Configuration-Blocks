package com.configurationblocks.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Per-dimension storage of block configurations. */
public class LevelConfigs extends SavedData {
    public static final String NAME = "configurationblocks_configs";
    public static final String FALLING_KEY = "cb_config";

    private final Map<BlockPos, BlockConfig> map = new HashMap<>();

    public static SavedData.Factory<LevelConfigs> factory() {
        return new SavedData.Factory<>(LevelConfigs::new, LevelConfigs::load, DataFixTypes.LEVEL);
    }

    public static LevelConfigs load(CompoundTag tag, HolderLookup.Provider registries) {
        LevelConfigs lc = new LevelConfigs();
        ListTag list = tag.getList("configs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            BlockPos pos = BlockPos.of(t.getLong("pos"));
            BlockConfig c = BlockConfig.load(t, registries);
            c.setDirtyCallback(lc::setDirty);
            lc.map.put(pos, c);
        }
        return lc;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, BlockConfig> e : map.entrySet()) {
            CompoundTag t = e.getValue().save(registries);
            t.putLong("pos", e.getKey().asLong());
            list.add(t);
        }
        tag.put("configs", list);
        return tag;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Map<BlockPos, BlockConfig> entries() {
        return map;
    }

    public BlockConfig get(BlockPos pos) {
        return map.get(pos);
    }

    public void put(BlockPos pos, BlockConfig cfg) {
        cfg.setDirtyCallback(this::setDirty);
        map.put(pos.immutable(), cfg);
        setDirty();
    }

    public BlockConfig remove(BlockPos pos) {
        BlockConfig c = map.remove(pos);
        if (c != null) setDirty();
        return c;
    }

    /** Called every server tick for the level. */
    public void tick(ServerLevel level) {
        if (map.isEmpty()) return;
        long time = level.getGameTime();

        for (Map.Entry<BlockPos, BlockConfig> e : map.entrySet()) {
            BlockConfig c = e.getValue();
            if (c.behavior.isFurnace() && c.container != null && level.hasChunkAt(e.getKey())) {
                FurnaceLogic.tick(level, c);
            } else if (c.behavior == Behavior.SPAWNER && level.hasChunkAt(e.getKey())) {
                SpawnerLogic.tick(level, e.getKey(), c);
            }
        }

        if (time % 5 != 0) return;
        for (Map.Entry<BlockPos, BlockConfig> e : new ArrayList<>(map.entrySet())) {
            BlockPos pos = e.getKey();
            BlockConfig cfg = e.getValue();
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);

            boolean gone = state.isAir();
            if (!gone && cfg.blockId != null
                    && !cfg.blockId.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
                gone = true;
            }
            if (gone) {
                purge(level, pos, cfg);
                continue;
            }

            if (cfg.gravity
                    && pos.getY() > level.getMinBuildHeight()
                    && FallingBlock.isFree(level.getBlockState(pos.below()))
                    && level.getBlockEntity(pos) == null) {
                map.remove(pos);
                setDirty();
                FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, state);
                fb.getPersistentData().put(FALLING_KEY, cfg.save(level.registryAccess()));
                if (cfg.light > 0) ConfigManager.clearLight(level, pos);
                ConfigManager.syncLevel(level);
                ConfigManager.syncPaint(level, pos);
                continue;
            }

            if (cfg.light > 0 && time % 20 == 0) {
                ConfigManager.refreshLight(level, pos, cfg.light);
            }
        }
    }

    private void purge(ServerLevel level, BlockPos pos, BlockConfig cfg) {
        if (cfg.behavior == Behavior.JUKEBOX && !cfg.disc.isEmpty()) level.levelEvent(1011, pos, 0);
        cfg.dropContents(level, pos);
        map.remove(pos);
        setDirty();
        if (cfg.light > 0) ConfigManager.clearLight(level, pos);
        ConfigManager.syncLevel(level);
        ConfigManager.syncPaint(level, pos);
    }
}
