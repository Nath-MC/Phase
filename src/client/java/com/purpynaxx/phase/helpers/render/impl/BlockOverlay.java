package com.purpynaxx.phase.helpers.render.impl;

import com.purpynaxx.phase.helpers.render.DrawMode;
import com.purpynaxx.phase.helpers.render.Renderable;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.awt.*;

public class BlockOverlay extends Renderable {

    private final Box box;

    /**
     * Constructs a new BlockOverlay.
     *
     * @param blockPos    The position of the block to overlay.
     * @param color       The color of the box.
     * @param drawMode    How the box should be drawn (filled, outline, or both).
     * @param ticksToLive The number of ticks this object should live before being removed (-1 for until manually removed).
     * @param debug       Whether this is overlay is used for debugging purposes.
     * @param depthTest   Whether this overlay should be depth tested.
     */
    public BlockOverlay(BlockPos blockPos, Color color, DrawMode drawMode, int ticksToLive, boolean debug, boolean depthTest) {
        super(color, drawMode, ticksToLive, debug, depthTest);
        this.box = new Box(blockPos);
        this.ticksToLive = ticksToLive;
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

}