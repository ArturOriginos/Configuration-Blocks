package com.configurationblocks.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.EnumMap;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** All custom properties of one placed block. */
public class BlockConfig {
    public static final float HARDNESS_UNSET = -2.0F;
    public static final float BLAST_UNSET = -1.0F;

    public float hardness = HARDNESS_UNSET;      // -2 = unset, -1 = unbreakable
    public float blastResistance = BLAST_UNSET;  // <0 = unset
    public boolean gravity;
    public Behavior behavior = Behavior.NONE;
    public int redstone;
    public int light;
    public float bounce;

    /** Block this config was made for. If the block at the position changes, the config is dropped. */
    public ResourceLocation blockId;

    /** Entity used when behavior == SPAWNER. */
    public static final ResourceLocation DEFAULT_SPAWN = ResourceLocation.parse("minecraft:pig");
    public ResourceLocation spawnerEntity = DEFAULT_SPAWN;
    public BaseSpawner spawner;             // runtime only
    public int spawnerMinDelay = 200;   // ticks
    public int spawnerMaxDelay = 800;   // ticks
    public int spawnerCount = 4;        // mobs per spawn attempt
    public int spawnerMaxNearby = 6;
    public int spawnerPlayerRange = 16;
    public int spawnerRange = 4;        // spawn radius

    // ---- storage settings ----
    public int storageRows = 3;     // chest only, 1..6 rows of 9 slots
    public boolean locked;          // only the owner (or ops) may use / edit / break
    public UUID owner;

    /** Painted pixels per face: 16x16 ARGB, 0 = transparent (shows the original texture). */
    public final EnumMap<Direction, int[]> paint = new EnumMap<>(Direction.class);

    public ConfigContainer container;
    public final SimpleContainerData furnaceData = new SimpleContainerData(4);
    public ItemStack disc = ItemStack.EMPTY;
    private Runnable dirtyCallback;

    public void setDirtyCallback(Runnable r) {
        this.dirtyCallback = r;
    }

    public boolean hasHardness() {
        return hardness > -1.5F;
    }

    public boolean isDefault() {
        return !hasHardness() && blastResistance < 0 && !gravity && behavior == Behavior.NONE
                && redstone == 0 && light == 0 && bounce == 0 && !locked && paint.isEmpty();
    }

    public void sanitize() {
        if (Float.isNaN(hardness) || hardness <= -2.0F) hardness = HARDNESS_UNSET;
        else if (hardness < 0) hardness = -1.0F;
        else hardness = Math.min(hardness, 1_000_000F);
        if (Float.isNaN(blastResistance) || blastResistance < 0) blastResistance = BLAST_UNSET;
        else blastResistance = Math.min(blastResistance, 3_600_000F);
        redstone = Mth.clamp(redstone, 0, 15);
        light = Mth.clamp(light, 0, 15);
        bounce = Float.isNaN(bounce) ? 0 : Mth.clamp(bounce, 0F, 10F);
        storageRows = Mth.clamp(storageRows, 1, 6);
        spawnerMinDelay = Mth.clamp(spawnerMinDelay, 1, 32000);
        spawnerMaxDelay = Mth.clamp(spawnerMaxDelay, spawnerMinDelay, 32000);
        spawnerCount = Mth.clamp(spawnerCount, 1, 64);
        spawnerMaxNearby = Mth.clamp(spawnerMaxNearby, 1, 500);
        spawnerPlayerRange = Mth.clamp(spawnerPlayerRange, 1, 128);
        spawnerRange = Mth.clamp(spawnerRange, 1, 16);
        if (spawnerEntity == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(spawnerEntity)
                || !BuiltInRegistries.ENTITY_TYPE.get(spawnerEntity).canSummon()) {
            spawnerEntity = DEFAULT_SPAWN;
        }
    }

    public void copyBasicFrom(BlockConfig o) {
        hardness = o.hardness;
        blastResistance = o.blastResistance;
        gravity = o.gravity;
        behavior = o.behavior;
        redstone = o.redstone;
        light = o.light;
        bounce = o.bounce;
        spawnerEntity = o.spawnerEntity;
        storageRows = o.storageRows;
        spawnerMinDelay = o.spawnerMinDelay;
        spawnerMaxDelay = o.spawnerMaxDelay;
        spawnerCount = o.spawnerCount;
        spawnerMaxNearby = o.spawnerMaxNearby;
        spawnerPlayerRange = o.spawnerPlayerRange;
        spawnerRange = o.spawnerRange;
        locked = o.locked;
        spawner = null;
    }

    public int slotCount() {
        return switch (behavior) {
            case CHEST -> storageRows * 9;
            case SHULKER_BOX -> 27;
            default -> behavior.containerSize();
        };
    }

    public boolean canAccess(Player p) {
        return !locked || owner == null || owner.equals(p.getUUID()) || p.hasPermissions(2);
    }

    public ConfigContainer ensureContainer() {
        int size = slotCount();
        if (size == 0) {
            container = null;
            return null;
        }
        if (container == null || container.getContainerSize() != size) {
            ConfigContainer nc = new ConfigContainer(size);
            nc.addListener(c -> {
                if (dirtyCallback != null) dirtyCallback.run();
            });
            if (container != null) {
                int n = Math.min(size, container.getContainerSize());
                for (int i = 0; i < n; i++) nc.setItem(i, container.getItem(i));
            }
            container = nc;
        }
        return container;
    }

    /** Drops items that no longer fit after slot count / stack limit changed. */
    public void fitContainer(Level level, BlockPos at) {
        if (container == null) return;
        int slots = slotCount();
        double x = at.getX() + 0.5, y = at.getY() + 0.5, z = at.getZ() + 0.5;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack st = container.getItem(i);
            if (st.isEmpty()) continue;
            if (i >= slots) {
                Containers.dropItemStack(level, x, y, z, st);
                container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public void resetState() {
        container = null;
        disc = ItemStack.EMPTY;
        spawner = null;
        for (int i = 0; i < 4; i++) furnaceData.set(i, 0);
    }

    /** Drops stored items (chest/furnace content, jukebox disc) at the given position. */
    public void dropContents(Level level, BlockPos at) {
        if (container != null) {
            Containers.dropContents(level, at, container);
            container.clearContent();
        }
        if (!disc.isEmpty()) {
            Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, disc);
            disc = ItemStack.EMPTY;
        }
    }

    // ---------- network (only the editable fields) ----------

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(hardness);
        buf.writeFloat(blastResistance);
        buf.writeBoolean(gravity);
        buf.writeVarInt(behavior.ordinal());
        buf.writeVarInt(redstone);
        buf.writeVarInt(light);
        buf.writeFloat(bounce);
        buf.writeResourceLocation(spawnerEntity);
        buf.writeVarInt(storageRows);
        buf.writeVarInt(spawnerMinDelay);
        buf.writeVarInt(spawnerMaxDelay);
        buf.writeVarInt(spawnerCount);
        buf.writeVarInt(spawnerMaxNearby);
        buf.writeVarInt(spawnerPlayerRange);
        buf.writeVarInt(spawnerRange);
        buf.writeBoolean(locked);
    }

    public static BlockConfig read(FriendlyByteBuf buf) {
        BlockConfig c = new BlockConfig();
        c.hardness = buf.readFloat();
        c.blastResistance = buf.readFloat();
        c.gravity = buf.readBoolean();
        c.behavior = Behavior.byOrdinal(buf.readVarInt());
        c.redstone = buf.readVarInt();
        c.light = buf.readVarInt();
        c.bounce = buf.readFloat();
        c.spawnerEntity = buf.readResourceLocation();
        c.storageRows = buf.readVarInt();
        c.spawnerMinDelay = buf.readVarInt();
        c.spawnerMaxDelay = buf.readVarInt();
        c.spawnerCount = buf.readVarInt();
        c.spawnerMaxNearby = buf.readVarInt();
        c.spawnerPlayerRange = buf.readVarInt();
        c.spawnerRange = buf.readVarInt();
        c.locked = buf.readBoolean();
        return c;
    }

    // ---------- NBT ----------

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag t = new CompoundTag();
        t.putFloat("hardness", hardness);
        t.putFloat("blast", blastResistance);
        t.putBoolean("gravity", gravity);
        t.putString("behavior", behavior.id);
        t.putInt("redstone", redstone);
        t.putInt("light", light);
        t.putFloat("bounce", bounce);
        t.putString("spawner_entity", spawnerEntity.toString());
        t.putInt("rows", storageRows);
        t.putInt("sp_min", spawnerMinDelay);
        t.putInt("sp_max", spawnerMaxDelay);
        t.putInt("sp_count", spawnerCount);
        t.putInt("sp_near", spawnerMaxNearby);
        t.putInt("sp_player", spawnerPlayerRange);
        t.putInt("sp_range", spawnerRange);
        t.putBoolean("locked", locked);
        if (owner != null) t.putUUID("owner", owner);
        if (!paint.isEmpty()) {
            CompoundTag pt = new CompoundTag();
            for (java.util.Map.Entry<Direction, int[]> e : paint.entrySet()) pt.putIntArray(e.getKey().getName(), e.getValue());
            t.put("paint", pt);
        }
        if (blockId != null) t.putString("block", blockId.toString());
        if (container != null) {
            NonNullList<ItemStack> list = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < list.size(); i++) list.set(i, container.getItem(i));
            CompoundTag items = new CompoundTag();
            ContainerHelper.saveAllItems(items, list, registries);
            t.put("items", items);
        }
        t.putIntArray("furnace", new int[]{furnaceData.get(0), furnaceData.get(1), furnaceData.get(2), furnaceData.get(3)});
        if (!disc.isEmpty()) t.put("disc", disc.save(registries));
        return t;
    }

    public static BlockConfig load(CompoundTag t, HolderLookup.Provider registries) {
        BlockConfig c = new BlockConfig();
        c.hardness = t.contains("hardness") ? t.getFloat("hardness") : HARDNESS_UNSET;
        c.blastResistance = t.contains("blast") ? t.getFloat("blast") : BLAST_UNSET;
        c.gravity = t.getBoolean("gravity");
        c.behavior = Behavior.byId(t.getString("behavior"));
        c.redstone = t.getInt("redstone");
        c.light = t.getInt("light");
        c.bounce = t.getFloat("bounce");
        c.storageRows = t.contains("rows") ? t.getInt("rows") : 3;
        if (t.contains("sp_min")) {
            c.spawnerMinDelay = t.getInt("sp_min");
            c.spawnerMaxDelay = t.getInt("sp_max");
            c.spawnerCount = t.getInt("sp_count");
            c.spawnerMaxNearby = t.getInt("sp_near");
            c.spawnerPlayerRange = t.getInt("sp_player");
            c.spawnerRange = t.getInt("sp_range");
        }
        c.locked = t.getBoolean("locked");
        if (t.hasUUID("owner")) c.owner = t.getUUID("owner");
        if (t.contains("paint")) {
            CompoundTag pt = t.getCompound("paint");
            for (Direction d : Direction.values()) {
                int[] arr = pt.getIntArray(d.getName());
                if (arr.length == 256) c.paint.put(d, arr);
            }
        }
        if (t.contains("spawner_entity")) {
            ResourceLocation se = ResourceLocation.tryParse(t.getString("spawner_entity"));
            if (se != null) c.spawnerEntity = se;
        }
        if (t.contains("block")) c.blockId = ResourceLocation.tryParse(t.getString("block"));
        if (c.ensureContainer() != null && t.contains("items")) {
            NonNullList<ItemStack> list = NonNullList.withSize(c.container.getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(t.getCompound("items"), list, registries);
            for (int i = 0; i < list.size(); i++) c.container.setItem(i, list.get(i));
        }
        int[] f = t.getIntArray("furnace");
        if (f.length == 4) for (int i = 0; i < 4; i++) c.furnaceData.set(i, f[i]);
        if (t.contains("disc")) c.disc = ItemStack.parse(registries, t.get("disc")).orElse(ItemStack.EMPTY);
        c.sanitize();
        return c;
    }
}
