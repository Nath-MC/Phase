package com.purpynaxx.phase.helpers.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import java.awt.*;

public abstract class Renderable {

    protected static final Renderer renderer = Renderer.getInstance();

    protected final Color color;
    protected final boolean debug;
    protected int ticksToLive;

    /**
     * Constructs a new Renderable.
     *
     * @param color       The color of the overlay.
     * @param ticksToLive The number of ticks this object should live before being removed (-1 for until manually removed).
     * @param debug       Whether this is overlay is used for debugging purposes.
     */
    public Renderable(Color color, int ticksToLive, boolean debug) {
        this.color = color;
        this.ticksToLive = ticksToLive;
        this.debug = debug;
    }

    public final boolean isExpired() {
        return ticksToLive <= 0;
    }

    public final void tick() {
        if (ticksToLive > 0) {
            ticksToLive--;
        }
    }

    public final boolean isDebug() {
        return debug;
    }

    abstract void render(WorldRenderContext context);

    public abstract boolean equals(Renderable renderable);

}
