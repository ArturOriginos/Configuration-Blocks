package com.configurationblocks.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToClient(OpenConfigPayload.TYPE, OpenConfigPayload.CODEC,
                (payload, ctx) -> com.configurationblocks.client.ClientHandler.open(payload));
        r.playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, ctx) -> com.configurationblocks.client.ClientHandler.sync(payload));
        r.playToClient(PaintSyncPayload.TYPE, PaintSyncPayload.CODEC,
                (payload, ctx) -> com.configurationblocks.client.ClientHandler.paintSync(payload));
        r.playToServer(PaintPayload.TYPE, PaintPayload.CODEC,
                (payload, ctx) -> ServerHandler.paint(payload, ctx));
        r.playToServer(ApplyConfigPayload.TYPE, ApplyConfigPayload.CODEC,
                (payload, ctx) -> ServerHandler.apply(payload, ctx));
    }
}
