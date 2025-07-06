package com.purpynaxx.phase.events.interfaces.client;

import net.minecraft.client.MinecraftClient;

public class ClientTick {

    public interface BEFORE {

        void beforeClientTick(MinecraftClient client);

    }

    public interface AFTER {

        void afterClientTick(MinecraftClient client);

    }

}
