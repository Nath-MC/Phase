package com.purpynaxx.phase.helpers.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.awt.*;

public class FaceOverlay extends Renderable {

    private final boolean fill;

    private final Direction direction;
    private final BlockPos blockPos;

    /**
     * Constructs a new Renderable.
     *
     * @param blockPos    The position of the block to overlay.
     * @param direction   The direction of the face to overlay.
     * @param color       The color of the overlay.
     * @param fill        Whether the overlay should be filled or just an outline.
     * @param ticksToLive The number of ticks this object should live before being removed (-1 for until manually removed).
     * @param debug       Whether this is overlay is used for debugging purposes.
     */
    public FaceOverlay(BlockPos blockPos, Direction direction, Color color, boolean fill, int ticksToLive, boolean debug) {
        super(color, ticksToLive, debug);
        this.blockPos = blockPos;
        this.direction = direction;
        this.fill = fill;
    }

    @Override
    public void render(WorldRenderContext context) {
        renderer.drawFace(context, blockPos, direction, color, fill);
    }

    @Override
    public boolean equals(Renderable renderable) {
        if (renderable instanceof FaceOverlay other) {
            return this.blockPos.equals(other.blockPos) && this.direction == other.direction;
        }
        return false;
    }

}
