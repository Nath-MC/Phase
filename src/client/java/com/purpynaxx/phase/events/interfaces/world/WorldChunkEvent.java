package com.purpynaxx.phase.events.interfaces.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;

public final class WorldChunkEvent {

    private WorldChunkEvent() {}

    public interface LOAD {

        void onChunkLoad(ClientWorld world, WorldChunk chunk);

    }

}
