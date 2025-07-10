package com.purpynaxx.phase.events.interfaces.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class WorldConnectivity {

    private WorldConnectivity() {}

    public interface JOIN {

        void onWorldJoin(ClientPlayNetworkHandler handler, Object sender, MinecraftClient client);

    }

    public interface LEAVE {

        void onWorldLeave(ClientPlayNetworkHandler handler, MinecraftClient client);

    }

}
