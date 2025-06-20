package com.purpynaxx.phase.events.interfaces.client;

import net.minecraft.client.MinecraftClient;

public interface ClientTick {

    interface BEFORE {

        void beforeClientTick(MinecraftClient client);

    }

    interface AFTER {

        void afterClientTick(MinecraftClient client);

    }

}
