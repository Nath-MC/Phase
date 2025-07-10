package com.purpynaxx.phase.helpers.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.modules.miscellaneous.Debug;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static net.minecraft.client.gl.RenderPipelines.MATRICES_COLOR_FOG_SNIPPET;
import static net.minecraft.client.gl.RenderPipelines.POSITION_COLOR_SNIPPET;
import static net.minecraft.client.render.RenderPhase.ITEM_ENTITY_TARGET;
import static net.minecraft.client.render.RenderPhase.VIEW_OFFSET_Z_LAYERING;

/**
 * Helper class for rendering visual debug elements.
 */
public final class Renderer {

    private static final Renderer INSTANCE = new Renderer();
    private static final Modules modules = Modules.getInstance();

    private final List<Renderable> renderables = new CopyOnWriteArrayList<>();

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

    public static boolean isDebug(Renderable renderable) {
        return renderable.isDebug();
    }

    /**
     * Adds a Renderable object to the render queue.
     *
     * @param renderable The Renderable object to be added.
     */
    public void addRenderable(Renderable renderable) {
        Objects.requireNonNull(renderable);

        renderables.removeIf(existing -> existing.equals(renderable));
        renderables.add(renderable);
    }

    /**
     * Renders all queued Renderable objects.
     *
     * @param renderContext The current world render context.
     */
    public void renderQueue(WorldRenderContext renderContext) {
        for (Renderable renderable : renderables) {
            if (renderable.isDebug() && !modules.isModuleActive(Debug.class)) continue;
            renderable.render(renderContext);
        }
    }

    /**
     * Updates the state of all Renderable objects in the queue.
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

        box = box.offset(renderContext.camera().getPos().negate());

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom face outline
        line(matrix, linesVertexConsumer, color, minX, minY, minZ, maxX, minY, minZ);
        line(matrix, linesVertexConsumer, color, maxX, minY, minZ, maxX, minY, maxZ);
        line(matrix, linesVertexConsumer, color, maxX, minY, maxZ, minX, minY, maxZ);
        line(matrix, linesVertexConsumer, color, minX, minY, maxZ, minX, minY, minZ);

        // Top face outline
        line(matrix, linesVertexConsumer, color, minX, maxY, minZ, maxX, maxY, minZ);
        line(matrix, linesVertexConsumer, color, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(matrix, linesVertexConsumer, color, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(matrix, linesVertexConsumer, color, minX, maxY, maxZ, minX, maxY, minZ);

        // Vertical edges outline
        line(matrix, linesVertexConsumer, color, minX, minY, minZ, minX, maxY, minZ);
        line(matrix, linesVertexConsumer, color, maxX, minY, minZ, maxX, maxY, minZ);
        line(matrix, linesVertexConsumer, color, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(matrix, linesVertexConsumer, color, minX, minY, maxZ, minX, maxY, maxZ);

        ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

        if (fill) {
            VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);

            // Down face (-Y)
            quad(matrix, quadsVertexConsumer, color,
                    maxX, minY, minZ,
                    maxX, minY, maxZ,
                    minX, minY, maxZ,
                    minX, minY, minZ
            );

            // Up face (+Y)
            quad(matrix, quadsVertexConsumer, color,
                    minX, maxY, maxZ,
                    maxX, maxY, maxZ,
                    maxX, maxY, minZ,
                    minX, maxY, minZ
            );

            // North face (-Z)
            quad(matrix, quadsVertexConsumer, color,
                    minX, maxY, minZ,
                    maxX, maxY, minZ,
                    maxX, minY, minZ,
                    minX, minY, minZ
            );

            // South face (+Z)
            quad(matrix, quadsVertexConsumer, color,
                    minX, minY, maxZ,
                    maxX, minY, maxZ,
                    maxX, maxY, maxZ,
                    minX, maxY, maxZ
            );

            // West face (-X)
            quad(matrix, quadsVertexConsumer, color,
                    minX, minY, maxZ,
                    minX, maxY, maxZ,
                    minX, maxY, minZ,
                    minX, minY, minZ
            );

            // East face (+X)
            quad(matrix, quadsVertexConsumer, color,
                    maxX, minY, minZ,
                    maxX, maxY, minZ,
                    maxX, maxY, maxZ,
                    maxX, minY, maxZ
            );

            ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
        }
    }

    /**
     * Draws a face of a block at the specified BlockPos with the given color.
     *
     * @param renderContext The current render context
     * @param blockPos      The world-space BlockPos where the face will be rendered.
     * @param direction     The direction of the face to be rendered.
     * @param color         The color for the face. The alpha component of the color isn't used if {@code fill} is false.
     * @param fill          Whether to fill the face with the color or just draw the outline.
     */
    public void drawFace(WorldRenderContext renderContext, BlockPos blockPos, Direction direction, Color color, boolean fill) {
        MatrixStack matrixStack = renderContext.matrixStack();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        Vec3d offset = renderContext.camera().getPos().negate();

        float minX = blockPos.getX() + (float) offset.x;
        float minY = blockPos.getY() + (float) offset.y;
        float minZ = blockPos.getZ() + (float) offset.z;

        float maxX = minX + 1.0F;
        float maxY = minY + 1.0F;
        float maxZ = minZ + 1.0F;

        VertexConsumer linesVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_LINES_LAYER);

        switch (direction) {
            case DOWN -> {
                line(matrix, linesVertexConsumer, color, minX, minY, minZ, maxX, minY, minZ);
                line(matrix, linesVertexConsumer, color, maxX, minY, minZ, maxX, minY, maxZ);
                line(matrix, linesVertexConsumer, color, maxX, minY, maxZ, minX, minY, maxZ);
                line(matrix, linesVertexConsumer, color, minX, minY, maxZ, minX, minY, minZ);

                ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

                if (fill) {
                    VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);
                    quad(matrix, quadsVertexConsumer, color,
                            maxX, minY, minZ,
                            maxX, minY, maxZ,
                            minX, minY, maxZ,
                            minX, minY, minZ
                    );
                    ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
                }
            }
            case UP -> {
                line(matrix, linesVertexConsumer, color, minX, maxY, minZ, maxX, maxY, minZ);
                line(matrix, linesVertexConsumer, color, maxX, maxY, minZ, maxX, maxY, maxZ);
                line(matrix, linesVertexConsumer, color, maxX, maxY, maxZ, minX, maxY, maxZ);
                line(matrix, linesVertexConsumer, color, minX, maxY, maxZ, minX, maxY, minZ);

                ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

                if (fill) {
                    VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);
                    quad(matrix, quadsVertexConsumer, color,
                            minX, maxY, maxZ,
                            maxX, maxY, maxZ,
                            maxX, maxY, minZ,
                            minX, maxY, minZ
                    );
                    ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
                }
            }
            case NORTH -> {
                line(matrix, linesVertexConsumer, color, minX, minY, minZ, maxX, minY, minZ);
                line(matrix, linesVertexConsumer, color, maxX, minY, minZ, maxX, maxY, minZ);
                line(matrix, linesVertexConsumer, color, maxX, maxY, minZ, minX, maxY, minZ);
                line(matrix, linesVertexConsumer, color, minX, maxY, minZ, minX, minY, minZ);

                ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

                if (fill) {
                    VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);
                    quad(matrix, quadsVertexConsumer, color,
                            minX, maxY, minZ,
                            maxX, maxY, minZ,
                            maxX, minY, minZ,
                            minX, minY, minZ
                    );
                    ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
                }
            }
            case SOUTH -> {
                line(matrix, linesVertexConsumer, color, minX, minY, maxZ, maxX, minY, maxZ);
                line(matrix, linesVertexConsumer, color, maxX, minY, maxZ, maxX, maxY, maxZ);
                line(matrix, linesVertexConsumer, color, maxX, maxY, maxZ, minX, maxY, maxZ);
                line(matrix, linesVertexConsumer, color, minX, maxY, maxZ, minX, minY, maxZ);

                ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

                if (fill) {
                    VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);
                    quad(matrix, quadsVertexConsumer, color,
                            minX, minY, maxZ,
                            maxX, minY, maxZ,
                            maxX, maxY, maxZ,
                            minX, maxY, maxZ
                    );
                    ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
                }
            }
            case WEST -> {
                line(matrix, linesVertexConsumer, color, minX, minY, minZ, minX, minY, maxZ);
                line(matrix, linesVertexConsumer, color, minX, minY, maxZ, minX, maxY, maxZ);
                line(matrix, linesVertexConsumer, color, minX, maxY, maxZ, minX, maxY, minZ);
                line(matrix, linesVertexConsumer, color, minX, maxY, minZ, minX, minY, minZ);

                ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

                if (fill) {
                    VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);
                    quad(matrix, quadsVertexConsumer, color,
                            minX, minY, maxZ,
                            minX, maxY, maxZ,
                            minX, maxY, minZ,
                            minX, minY, minZ
                    );
                    ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
                }
            }
            case EAST -> {
                line(matrix, linesVertexConsumer, color, maxX, minY, minZ, maxX, minY, maxZ);
                line(matrix, linesVertexConsumer, color, maxX, minY, maxZ, maxX, maxY, maxZ);
                line(matrix, linesVertexConsumer, color, maxX, maxY, maxZ, maxX, maxY, minZ);
                line(matrix, linesVertexConsumer, color, maxX, maxY, minZ, maxX, minY, minZ);

                ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();

                if (fill) {
                    VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(Layers.OVERLAY_QUADS_LAYER);
                    quad(matrix, quadsVertexConsumer, color,
                            maxX, minY, minZ,
                            maxX, maxY, minZ,
                            maxX, maxY, maxZ,
                            maxX, minY, maxZ
                    );
                    ((VertexConsumerProvider.Immediate) renderContext.consumers()).drawCurrentLayer();
                }
            }
        }
    }

    /**
     * Helper method to draw a quad (four vertices).
     *
     * @param matrix   The transformation matrix.
     * @param consumer The vertex consumer to draw to.
     * @param color    The color of the quad.
     * @param x1       X-coordinate of the first vertex.
     * @param y1       Y-coordinate of the first vertex.
     * @param z1       Z-coordinate of the first vertex.
     * @param x2       X-coordinate of the second vertex.
     * @param y2       Y-coordinate of the second vertex.
     * @param z2       Z-coordinate of the second vertex.
     * @param x3       X-coordinate of the third vertex.
     * @param y3       Y-coordinate of the third vertex.
     * @param z3       Z-coordinate of the third vertex.
     * @param x4       X-coordinate of the fourth vertex.
     * @param y4       Y-coordinate of the fourth vertex.
     * @param z4       Z-coordinate of the fourth vertex.
     */
    private void quad(Matrix4f matrix, VertexConsumer consumer, Color color,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4) {

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
        consumer.vertex(matrix, x3, y3, z3).color(r, g, b, a);
        consumer.vertex(matrix, x4, y4, z4).color(r, g, b, a);
    }

    /**
     * Helper method to draw a line (two vertices).
     *
     * @param matrix   The transformation matrix.
     * @param consumer The vertex consumer to draw to.
     * @param color    The color of the line.
     * @param x1       X-coordinate of the first vertex.
     * @param y1       Y-coordinate of the first vertex.
     * @param z1       Z-coordinate of the first vertex.
     * @param x2       X-coordinate of the second vertex.
     * @param y2       Y-coordinate of the second vertex.
     * @param z2       Z-coordinate of the second vertex.
     */
    private void line(Matrix4f matrix, VertexConsumer consumer, Color color,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2) {

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, 255);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, 255);
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
