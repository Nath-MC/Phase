package com.purpynaxx.phase.events.interfaces.world;

import net.minecraft.client.world.ClientWorld;

public class WorldTick {

    public interface BEFORE {

        void beforeWorldTick(ClientWorld world);

    }

    public interface AFTER {

        void afterWorldTick(ClientWorld world);

    }

}
