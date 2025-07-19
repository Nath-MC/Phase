package com.purpynaxx.phase.helpers.render.impl;

import com.purpynaxx.phase.helpers.render.Renderable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.Optional;

public class FaceOverlay extends Renderable {

    private final Box box;

    private FaceOverlay(Builder builder) {
        super(builder);
        this.box = getBox(
                builder.blockPos.orElseThrow(),
                builder.direction.orElse(Direction.UP)
        );
    }

    private static Box getBox(BlockPos blockPos, Direction direction) {
        Box box = new Box(blockPos).offset(0.05, 0.001, 0.05).shrink(0.1, 0, 0.1);

        return switch (direction) {
            case UP -> box.offset(0, 1, 0).shrink(0, 0.9, 0);
            case DOWN -> box.offset(0, -1, 0).shrink(0, -0.9, 0);
            case NORTH -> box.offset(0, 0, -1).shrink(0, 0, -0.9);
            case SOUTH -> box.offset(0, 0, 1).shrink(0, 0, 0.9);
            case WEST -> box.offset(-1, 0, 0).shrink(-0.9, 0, 0);
            case EAST -> box.offset(1, 0, 0).shrink(0.9, 0, 0);
        };
    }

    @Override
    public void render(WorldRenderContext context) {
        renderer.drawBox(context, box, color, drawMode, depthTest);
    }

    @Override
    public boolean equals(Renderable renderable) {
        return renderable instanceof FaceOverlay other && this.box.equals(other.box);
    }

    public static class Builder extends Renderable.Builder<Builder> {

        private Optional<BlockPos> blockPos = Optional.empty();
        private Optional<Direction> direction = Optional.empty();

        public Builder blockPos(BlockPos blockPos) {
            this.blockPos = Optional.of(blockPos);
            return this;
        }

        public Builder direction(Direction direction) {
            this.direction = Optional.of(direction);
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FaceOverlay build() {
            if (blockPos.isEmpty()) {
                throw new IllegalArgumentException("BlockPos cannot be empty");
            }

            return new FaceOverlay(this);
        }

    }

}
