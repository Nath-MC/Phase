package com.purpynaxx.phase.events.listeners;

import net.minecraft.client.MinecraftClient;

public interface ClientTickStartListener {
    void onClientTickStart(MinecraftClient client);
}
