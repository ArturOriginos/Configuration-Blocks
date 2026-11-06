package com.configurationblocks.config;

import com.configurationblocks.network.PaintSyncPayload;
import com.configurationblocks.network.SyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ConfigManager {
    private ConfigManager() {}

    public static LevelConfigs get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LevelConfigs.factory(), LevelConfigs.NAME);
    }

    // ---- client sync (hardness + "interactive" flag) ----

    public static SyncPayload buildSync(ServerLevel level) {
        Map<BlockPos, SyncPayload.Entry> m = new HashMap<>();
        for (Map.Entry<BlockPos, BlockConfig> e : get(level).entries().entrySet()) {
            BlockConfig c = e.getValue();
            if (c.hasHardness() || c.behavior.isInteractive()) {
                m.put(e.getKey(), new SyncPayload.Entry(c.hardness, c.behavior.isInteractive()));
            }
        }
        return new SyncPayload(level.dimension(), m);
    }

    public static void syncLevel(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, buildSync(level));
    }

    public static void syncPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSync(player.serverLevel()));
        syncPaintFull(player);
    }

    /** Call after the config map of a position changed. */
    public static void applyChange(ServerLevel level, BlockPos pos) {
        BlockConfig now = get(level).get(pos);
        if (now == null || now.light == 0) clearLight(level, pos);
        else refreshLight(level, pos, now.light);
        level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        syncLevel(level);
        syncPaint(level, pos);
    }

    // ---- paint sync ----

    /** Sends the painting of one block position (empty if none) to everybody in the dimension. */
    public static void syncPaint(ServerLevel level, BlockPos pos) {
        BlockConfig c = get(level).get(pos);
        Map<BlockPos, Map<Direction, int[]>> m = new HashMap<>();
        Map<Direction, int[]> faces = new EnumMap<>(Direction.class);
        if (c != null) faces.putAll(c.paint);
        m.put(pos.immutable(), faces);
        PacketDistributor.sendToPlayersInDimension(level, new PaintSyncPayload(level.dimension(), false, m));
    }

    /** Full paint sync for one player, in batches to stay far below the packet size limit. */
    public static void syncPaintFull(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Map<BlockPos, Map<Direction, int[]>> batch = new HashMap<>();
        boolean first = true;
        for (Map.Entry<BlockPos, BlockConfig> e : get(level).entries().entrySet()) {
            if (e.getValue().paint.isEmpty()) continue;
            batch.put(e.getKey(), new EnumMap<>(e.getValue().paint));
            if (batch.size() >= 40) {
                PacketDistributor.sendToPlayer(player, new PaintSyncPayload(level.dimension(), first, batch));
                first = false;
                batch = new HashMap<>();
            }
        }
        if (first || !batch.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new PaintSyncPayload(level.dimension(), first, batch));
        }
    }

    // ---- light: invisible vanilla light blocks placed in the air next to the block ----

    public static void refreshLight(ServerLevel level, BlockPos pos, int lvl) {
        BlockState ls = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lvl);
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.hasChunkAt(n)) continue;
            BlockState s = level.getBlockState(n);
            if ((s.isAir() || s.is(Blocks.LIGHT)) && !s.equals(ls)) {
                level.setBlock(n, ls, 2);
            }
        }
    }

    public static void clearLight(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (!level.hasChunkAt(n)) continue;
            if (level.getBlockState(n).is(Blocks.LIGHT)) {
                level.setBlock(n, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}
