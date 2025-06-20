package com.purpynaxx.phase.events.interfaces.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface WorldConnectivity {

    interface JOIN {

        void onWorldJoin(ClientPlayNetworkHandler handler, Object sender, MinecraftClient client);

    }

    interface LEAVE {

        void onWorldLeave(ClientPlayNetworkHandler handler, MinecraftClient client);

    }

}
