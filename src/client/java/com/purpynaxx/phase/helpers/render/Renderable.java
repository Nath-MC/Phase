package com.purpynaxx.phase.helpers.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import java.awt.*;

public abstract class Renderable {

    protected static final Renderer renderer = Renderer.getInstance();

    protected final Color color;

    protected final DrawMode drawMode;

    protected final boolean debug;
    protected final boolean depthTest;

    protected int ticksToLive;

    /**
     * Constructs a new Renderable.
     *
     * @param color       The color of the overlay.
     * @param drawMode    How the overlay should be drawn (filled, outline, or both).
     * @param ticksToLive The number of ticks this object should live before being removed (-1 for until manually removed).
     * @param debug       Whether this is overlay is used for debugging purposes.
     * @param depthTest   Whether this overlay should be depth tested.
     */
    public Renderable(Color color, DrawMode drawMode, int ticksToLive, boolean debug, boolean depthTest) {
        this.color = color;
        this.drawMode = drawMode;
        this.ticksToLive = Math.max(-1, ticksToLive);
        this.debug = debug;
        this.depthTest = depthTest;
    }

    public final boolean isExpired() {
        return ticksToLive == 0;
    }

    public final void tick() {
        if (ticksToLive > 0) {
            ticksToLive--;
        }
    }

    public final boolean isDebug() {
        return debug;
    }

    protected abstract void render(WorldRenderContext context);

    public abstract boolean equals(Renderable renderable);

}
