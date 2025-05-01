package com.purpynaxx.phase.events.listeners;

import net.minecraft.client.world.ClientWorld;

public interface WorldTickEndListener {
    void onWorldTickEnd(ClientWorld world);
}
