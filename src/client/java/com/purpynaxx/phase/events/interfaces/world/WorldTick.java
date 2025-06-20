package com.purpynaxx.phase.events.interfaces.world;

import net.minecraft.client.world.ClientWorld;

public interface WorldTick {

    interface BEFORE {

        void beforeWorldTick(ClientWorld world);

    }

    interface AFTER {

        void afterWorldTick(ClientWorld world);

    }

}
