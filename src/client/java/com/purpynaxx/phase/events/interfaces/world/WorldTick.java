package com.purpynaxx.phase.events.interfaces.world;

import net.minecraft.client.world.ClientWorld;

public final class WorldTick {

    private WorldTick() {}

    public interface BEFORE {

        void beforeWorldTick(ClientWorld world);

    }

    public interface AFTER {

        void afterWorldTick(ClientWorld world);

    }

}
