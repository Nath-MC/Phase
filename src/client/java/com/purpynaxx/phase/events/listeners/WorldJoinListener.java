package com.purpynaxx.phase.events.listeners;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface WorldJoinListener {
    void onWorldJoin(ClientPlayNetworkHandler handler, Object sender, MinecraftClient client);
}
