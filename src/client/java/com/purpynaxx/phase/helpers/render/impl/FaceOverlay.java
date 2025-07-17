package com.purpynaxx.phase.helpers.render.impl;

import com.purpynaxx.phase.helpers.render.DrawMode;
import com.purpynaxx.phase.helpers.render.Renderable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;

public class FaceOverlay extends Renderable {

    private final Box box;

    /**
     * Constructs a new Renderable.
     *
     * @param blockPos    The position of the block to overlay.
     * @param direction   The direction of the face to overlay.
     * @param color       The color of the overlay.
     * @param drawMode    How the overlay should be drawn (filled, outline, or both).
     * @param ticksToLive The number of ticks this object should live before being removed (-1 for until manually removed).
     * @param debug       Whether this is overlay is used for debugging purposes.
     * @param depthTest   Whether this overlay should be depth tested.
     */
    public FaceOverlay(BlockPos blockPos, Direction direction, Color color, DrawMode drawMode, int ticksToLive, boolean debug, boolean depthTest) {
        super(color, drawMode, ticksToLive, debug, depthTest);
        this.box = getBox(blockPos, direction);
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

}
