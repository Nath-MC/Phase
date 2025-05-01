package com.purpynaxx.phase.events.listeners;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface WorldLeaveListener {
    void onWorldLeave(ClientPlayNetworkHandler handler, MinecraftClient client);
}
