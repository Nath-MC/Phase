package com.purpynaxx.phase.helpers.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface Renderable {

    /**
     * Renders the object in the world.
     *
     * @param context The current world render context.
     */
    void render(WorldRenderContext context);


    void tick();

    /**
     * Checks if the object's time-to-live has expired.
     *
     * @return true if the object should be removed from the queue, false otherwise.
     */
    boolean isExpired();

}