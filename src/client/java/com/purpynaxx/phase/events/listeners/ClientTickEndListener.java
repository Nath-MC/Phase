package com.purpynaxx.phase.events.listeners;

import net.minecraft.client.MinecraftClient;

public interface ClientTickEndListener {
    void onClientTickEnd(MinecraftClient client);
}
