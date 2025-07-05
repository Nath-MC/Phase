package com.purpynaxx.phase.helpers.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

public class BlockOverlay implements Renderable {

    private static final Renderer renderer = Renderer.getInstance();

    private final BlockPos blockPos;

    private final Color color;

    private final boolean fill;

    private int ticksToLive;

    private final boolean debug;



    /**
     * Constructs a new BlockOverlay.
     *
     * @param blockPos    The position of the block to overlay.
     * @param color       The color of the box.
     * @param fill        Whether the box should be filled or just an outline.
     * @param ticksToLive The number of ticks this object should live before being removed (-1 for until manually removed).
     * @param debug       Whether this is overlay is used for debugging purposes.
     */
    public BlockOverlay(BlockPos blockPos, Color color, boolean fill, int ticksToLive, boolean debug) {
        this.blockPos = blockPos;
        this.color = color;
        this.fill = fill;
        this.ticksToLive = ticksToLive;
        this.debug = debug;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public void tick() {
        if (ticksToLive > 0) {
            ticksToLive--;
        }
    }

    @Override
    public void render(WorldRenderContext context) {
        renderer.drawBox(context, blockPos, color, fill);
    }

    @Override
    public boolean isExpired() {
        return ticksToLive == 0;
    }

}