package com.purpynaxx.phase.events.listeners;

import net.minecraft.client.world.ClientWorld;

public interface WorldStartTickListener {
    void onWorldTickStart(ClientWorld world);
}
