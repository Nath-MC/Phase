package com.purpynaxx.phase.helpers.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Predicate;

import static net.minecraft.client.gl.RenderPipelines.MATRICES_COLOR_FOG_SNIPPET;
import static net.minecraft.client.gl.RenderPipelines.POSITION_COLOR_SNIPPET;
import static net.minecraft.client.render.RenderPhase.ITEM_ENTITY_TARGET;
import static net.minecraft.client.render.RenderPhase.VIEW_OFFSET_Z_LAYERING;

/**
 * Helper class for rendering visual debug elements.
 * It provides static methods to draw boxes directly in the world.
 */
public final class Renderer {

    private static final Renderer INSTANCE = new Renderer();
    private final List<Renderable> renderables = new ArrayList<>();

    private final List<AbstractRenderable> renderables = new CopyOnWriteArrayList<>();

    private Renderer() {}

    public static Renderer getInstance() {
        return INSTANCE;
    }

    public static void positionMatrixAndRender(WorldRenderContext context, Runnable runnable) {
        MatrixStack matrixStack = context.matrixStack();
        Quaternionf cameraRotation = context.camera().getRotation().invert(new Quaternionf());

        matrixStack.push();
        matrixStack.multiply(cameraRotation);

        runnable.run();

        matrixStack.pop();
    }

    /**
     * Adds a Renderable object to the render queue.
     *
     * @param renderable The Renderable object to be added.
     */
    public void addRenderable(Renderable renderable) {
        if (renderable == null) return;
        renderables.removeIf(existing -> {
            if (existing instanceof BlockOverlay blockOverlay && renderable instanceof BlockOverlay newBlockOverlay) {
                return blockOverlay.getBlockPos().equals(newBlockOverlay.getBlockPos());
            }
            return existing.equals(renderable);
        });
        renderables.add(renderable);
    }

    /**
     * Renders all queued Renderable objects.
     *
     * @param renderContext The current world render context.
     */
    public void renderQueue(WorldRenderContext renderContext) {
        renderables.forEach(renderable -> renderable.render(renderContext));
    }

    /**
     * Updates the state of all Renderable objects in the queue.
     * This method should be called every tick to update the renderables.
     */
    public void tick() {
        renderables.forEach(Renderable::tick);
        renderables.removeIf(Renderable::isExpired);
    }

    /**
     * Clears the render queue, removing all Renderable objects.
     */
    public void clearQueue() {
        renderables.clear();
    }

    /**
     * Removes all Renderable objects that match the given predicate from the render queue.
     *
     * @param predicate The predicate to test each Renderable object against.
     */
    public void removeRenderablesIf(Predicate<Renderable> predicate) {
        renderables.removeIf(predicate);
    }


    /**
     * Draws a filled box in the world at the specified Entity's bounding box with the given color.
     *
     * @param renderContext The current render context
     * @param entity        The Entity whose bounding box will be rendered as a Box.
     * @param color         The color for the box. The alpha component of the color isn't used.
     * @param fill          Whether to fill the box with the color or just draw the outline.
     */
    public void drawBoxForEntity(WorldRenderContext renderContext, Entity entity, Color color, boolean fill) {
        RenderTickCounter renderTickCounter = renderContext.tickCounter();
        float progress = renderTickCounter.getTickProgress(true);

        double x = MathHelper.lerp(progress, entity.lastRenderX, entity.getX()) - entity.getX();
        double y = MathHelper.lerp(progress, entity.lastRenderY, entity.getY()) - entity.getY();
        double z = MathHelper.lerp(progress, entity.lastRenderZ, entity.getZ()) - entity.getZ();

        Box box = entity.getBoundingBox().expand(0.1D).offset(x, y, z);

        drawBox(renderContext, box, color, fill);
    }

    /**
     * Draws a box in the world at the specified BlockPos with the given color.
     *
     * @param renderContext The current render context
     * @param blockPos      The world-space BlockPos to be rendered as a Box.
     * @param color         The color for the box. The alpha component of the color isn't used if {@code fill} is false.
     * @param fill          Whether to fill the box with the color or just draw the outline.
     */
    public void drawBox(WorldRenderContext renderContext, BlockPos blockPos, Color color, boolean fill) {
        drawBox(renderContext, new Box(blockPos), color, fill);
    }

    /**
     * Draws a box in the world at the specified Box with the given color.
     *
     * @param renderContext The current render context
     * @param box           The world-space Box to be rendered.
     * @param color         The color for the box. The alpha component of the color isn't used if {@code fill} is false.
     * @param fill          Whether to fill the box with the color or just draw the outline.
     */
    public void drawBox(WorldRenderContext renderContext, Box box, Color color, boolean fill) {
        MatrixStack matrixStack = renderContext.matrixStack();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        VertexConsumer linesVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_LINES_LAYER);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        box = box.offset(renderContext.camera().getPos().negate());

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom face
        linesVertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, 255);

        // Top face
        linesVertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, 255);

        // Vertical edges
        linesVertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, 255);

        linesVertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, 255);
        linesVertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, 255);

        ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

        if (fill) {
            VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);

            // Down face (-Y)
            quadsVertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

            // Up face (+Y)
            quadsVertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

            // North face (-Z)
            quadsVertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

            // South face (+Z)
            quadsVertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

            // West face (-X)
            quadsVertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);

            // East face (+X)
            quadsVertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
            quadsVertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

            ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
        }
    }


    private static class Layers {

        private static final RenderPipeline.Snippet RENDERTYPE_OVERLAY_LINES_SNIPPET = RenderPipeline.builder(MATRICES_COLOR_FOG_SNIPPET)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withUniform("LineWidth", UniformType.FLOAT)
                .withUniform("ScreenSize", UniformType.VEC2)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.LINES)
                .buildSnippet();

        private static final RenderPipeline OVERLAY_LINES = RenderPipelines.register(
                RenderPipeline.builder(RENDERTYPE_OVERLAY_LINES_SNIPPET)
                        .withLocation("pipeline/lines")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .build()
        );

        private static final RenderLayer.MultiPhase OVERLAY_LINES_LAYER = RenderLayer.MultiPhase.of(
                "overlay_lines",
                1536,
                OVERLAY_LINES,
                RenderLayer.MultiPhaseParameters.builder()
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3.0D)))
                        .layering(VIEW_OFFSET_Z_LAYERING)
                        .target(ITEM_ENTITY_TARGET)
                        .build(false)
        );

        private static final RenderPipeline OVERLAY_QUADS = RenderPipelines.register(
                RenderPipeline.builder(POSITION_COLOR_SNIPPET)
                        .withLocation("pipeline/debug_quads")
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .build()
        );

        private static final RenderLayer.MultiPhase OVERLAY_QUADS_LAYER = RenderLayer.of(
                "overlay_quads",
                1536,
                false,
                true,
                OVERLAY_QUADS,
                RenderLayer.MultiPhaseParameters.builder()
                        .target(ITEM_ENTITY_TARGET)
                        .layering(VIEW_OFFSET_Z_LAYERING)
                        .build(false)
        );

    }

}