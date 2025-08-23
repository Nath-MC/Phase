package com.purpynaxx.phase.render.impl;

import com.purpynaxx.phase.render.Renderable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class PathLine extends Renderable {

    private final Vec3d start;
    private final Vec3d end;

    private PathLine(Builder builder) {
        super(builder);
        this.start = builder.start.orElseThrow(() -> new IllegalArgumentException("Start position has not been specified !"));
        this.end = builder.end.orElseThrow(() -> new IllegalArgumentException("End position has not been specified !"));
    }

    @Override
    protected void render(WorldRenderContext context) {
        renderer.drawLine(context, start, end, color, depthTest);
    }

    @Override
    public boolean equals(Renderable renderable) {
        if (renderable instanceof PathLine other) {
            return start.equals(other.start) && end.equals(other.end);
        } else return false;
    }

    public static class Builder extends Renderable.Builder<Builder> {

        private Optional<Vec3d> start = Optional.empty();
        private Optional<Vec3d> end = Optional.empty();

        public Builder start(Vec3d start) {
            this.start = Optional.of(start);
            return this;
        }

        public Builder end(Vec3d end) {
            this.end = Optional.of(end);
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public PathLine build() {
            return new PathLine(this);
        }

    }


}
