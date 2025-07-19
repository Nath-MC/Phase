package com.purpynaxx.phase.helpers.render.impl;

import com.purpynaxx.phase.helpers.render.Renderable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Optional;

public class BlockOverlay extends Renderable {

    private final Box box;

    private BlockOverlay(Builder builder) {
        super(builder);
        this.box = new Box(builder.blockPos.orElseThrow());
    }

    @Override
    public void render(WorldRenderContext context) {
        renderer.drawBox(context, box, color, drawMode, depthTest);
    }

    @Override
    public boolean equals(Renderable renderable) {
        if (renderable instanceof BlockOverlay other) {
            return this.box.equals(other.box);
        }
        return false;
    }

    public static class Builder extends Renderable.Builder<Builder> {

        private Optional<BlockPos> blockPos = Optional.empty();

        public Builder blockPos(BlockPos blockPos) {
            this.blockPos = Optional.of(blockPos);
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public BlockOverlay build() {
            if (blockPos.isEmpty()) {
                throw new IllegalArgumentException("BlockPos cannot be empty");
            }

            return new BlockOverlay(this);
        }

    }

}