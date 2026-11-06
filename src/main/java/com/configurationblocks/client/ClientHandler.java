package com.configurationblocks.client;

import com.configurationblocks.config.ClientConfigCache;
import com.configurationblocks.network.OpenConfigPayload;
import com.configurationblocks.network.SyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/** Client-only code; only ever loaded on the physical client. */
public final class ClientHandler {
    private ClientHandler() {}

    public static void open(OpenConfigPayload p) {
        Minecraft mc = Minecraft.getInstance();
        Component name = mc.level != null
                ? mc.level.getBlockState(p.pos()).getBlock().getName()
                : Component.empty();
        mc.setScreen(new ConfiguratorScreen(p.pos(), p.face(), p.config(), name));
    }

    public static void paintSync(com.configurationblocks.network.PaintSyncPayload p) {
        PaintClient.apply(p);
    }

    public static void sync(SyncPayload p) {
        Map<BlockPos, ClientConfigCache.Entry> m = new HashMap<>();
        p.entries().forEach((pos, e) -> m.put(pos, new ClientConfigCache.Entry(e.hardness(), e.interactive())));
        ClientConfigCache.set(p.dimension(), m);
    }
}
