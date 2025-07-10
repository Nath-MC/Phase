package com.purpynaxx.phase.events.interfaces.world;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public final class WorldRender {

    private WorldRender() {}

    /**
     * Called after all world rendering is done.
     * Mainly for GUI-like rendering.
     * The view matrix will be modified to match the current camera rotation before this event is invoked.
     *
     * @see net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.End
     */
    public interface END {

        void onWorldRenderEnd(WorldRenderContext context);

    }

}
