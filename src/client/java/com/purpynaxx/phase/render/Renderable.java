package com.purpynaxx.phase.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import java.awt.*;
import java.util.Optional;

public abstract class Renderable {

    protected static final Renderer renderer = Renderer.getInstance();
    private static final Color DEFAULT_COLOR = new Color(112, 112, 112, 178);

    protected final Color color;

    protected final DrawMode drawMode;

    protected final boolean debug;
    protected final boolean depthTest;

    protected int ticksToLive;

    protected Renderable(Builder<?> builder) {
        this.color = builder.color.orElse(DEFAULT_COLOR);
        this.drawMode = builder.drawMode.orElse(DrawMode.BOTH);
        this.ticksToLive = builder.ticksToLive.orElse(-1);
        this.debug = builder.debug.orElse(false);
        this.depthTest = builder.depthTest.orElse(true);
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

    protected static abstract class Builder<T extends Builder<T>> {

        private Optional<Color> color = Optional.empty();
        private Optional<DrawMode> drawMode = Optional.empty();
        private Optional<Boolean> debug = Optional.empty();
        private Optional<Boolean> depthTest = Optional.empty();
        private Optional<Integer> ticksToLive = Optional.empty();

        public T color(Color color) {
            this.color = Optional.of(color);
            return self();
        }

        public T drawMode(DrawMode drawMode) {
            this.drawMode = Optional.of(drawMode);
            return self();
        }

        public T debug(boolean debug) {
            this.debug = Optional.of(debug);
            return self();
        }

        public T depthTest(boolean depthTest) {
            this.depthTest = Optional.of(depthTest);
            return self();
        }

        public T ticksToLive(int ticksToLive) {
            this.ticksToLive = Optional.of(Math.max(-1, ticksToLive));
            return self();
        }

        protected abstract T self();

        public abstract Renderable build();

    }

}
