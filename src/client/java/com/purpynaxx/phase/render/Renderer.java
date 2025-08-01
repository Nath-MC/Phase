package com.purpynaxx.phase.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.purpynaxx.phase.modules.Module;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.modules.miscellaneous.Debug;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.minecraft.client.render.RenderPhase.ITEM_ENTITY_TARGET;
import static net.minecraft.client.render.RenderPhase.VIEW_OFFSET_Z_LAYERING;

/**
 * Helper class for rendering visual debug elements.
 */
public final class Renderer {

    private static final Renderer INSTANCE = new Renderer();
    private static final Modules modules = Modules.getInstance();

    private final Map<Module, List<Renderable>> renderablesByModuleMap = new ConcurrentHashMap<>();

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
     * @param module The module instance which own the renderable
     * @param renderable The Renderable object to be added.
     */
    public void addRenderable(Module module, Renderable renderable) {
        if (renderablesByModuleMap.putIfAbsent(module, new CopyOnWriteArrayList<>()) != null) {
            getRenderables(module).orElseThrow().removeIf(existing -> existing.equals(renderable));
        }
        getRenderables(module).orElseThrow().add(renderable);
    }

    private void forAllRenderables(Consumer<Renderable> consumer) {
        for (List<Renderable> renderables : renderablesByModuleMap.values())
            for (Renderable renderable : renderables)
                consumer.accept(renderable);
    }

    /**
     * Clears all renderable objects
     */
    public void clear() {
        renderablesByModuleMap.clear();
    }

    /**
     * Clears all renderable objects owned by {@code module}
     *
     * @param module The module instance
     */
    public void clear(Module module) {
        getRenderables(module).ifPresent(List::clear);
    }

    private Optional<List<Renderable>> getRenderables(Module module) {
        return Optional.ofNullable(renderablesByModuleMap.get(module));
    }

    /**
     * Removes all renderable objects owned by {@code module} and matching the predicate {@code filter}
     *
     * @param module The module instance
     * @param filter A predicate which returns {@code true} for renderables to be removed
     */
    public void removeIf(Module module, Predicate<Renderable> filter) {
        getRenderables(module).ifPresent(renderables -> renderables.removeIf(filter));
    }

    /**
     * Renders all queued Renderable objects.
     *
     * @param renderContext The current world render context.
     */
    public void render(WorldRenderContext renderContext) {
        forAllRenderables(renderable -> {
            if (renderable.isDebug() && !modules.isModuleActive(Debug.class)) return;
            renderable.render(renderContext);
        });
    }

    /**
     * Updates the state of all Renderable objects in the queue.
     */
    public void tick() {
        forAllRenderables(Renderable::tick);
        renderablesByModuleMap.values().forEach(renderables -> renderables.removeIf(Renderable::isExpired));
    }


    /**
     * Draws a filled box in the world at the specified Entity's bounding box with the given color.
     *
     * @param renderContext The current render context
     * @param entity        The Entity whose bounding box will be rendered as a Box.
     * @param color         The color for the box. The alpha component of the color isn't used.
     * @param mode          The mode for drawing the box (outline, fill, or both).
     * @param depthTest     Whether to perform depth testing when rendering the box.
     */
    public void drawBoxForEntity(WorldRenderContext renderContext, Entity entity, Color color, DrawMode mode, boolean depthTest) {
        RenderTickCounter renderTickCounter = renderContext.tickCounter();
        float progress = renderTickCounter.getTickProgress(true);

        double x = MathHelper.lerp(progress, entity.lastRenderX, entity.getX()) - entity.getX();
        double y = MathHelper.lerp(progress, entity.lastRenderY, entity.getY()) - entity.getY();
        double z = MathHelper.lerp(progress, entity.lastRenderZ, entity.getZ()) - entity.getZ();

        Box box = entity.getBoundingBox().expand(0.1D).offset(x, y, z);

        drawBox(renderContext, box, color, mode, depthTest);
    }

    /**
     * Draws a box in the world at the specified Box with the given color.
     *
     * @param renderContext The current render context
     * @param box           The world-space Box to be rendered.
     * @param color         The color for the box. The alpha component of the color isn't used if {@code fill} is false.
     * @param mode          The mode for drawing the box (outline, fill, or both).
     * @param depthTest     Whether to perform depth testing when rendering the box.
     */
    public void drawBox(WorldRenderContext renderContext, Box box, Color color, DrawMode mode, boolean depthTest) {
        MatrixStack matrixStack = renderContext.matrixStack();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        box = box.offset(renderContext.camera().getPos().negate());

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        if (mode.isOutline()) {
            RenderLayer layer = depthTest ? Layers.LINES_LAYER : Layers.OVERLAY_LINES_LAYER;
            VertexConsumer linesVertexConsumer = renderContext.consumers().getBuffer(layer);

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
        }

        if (mode.isFill()) {
            RenderLayer layer = depthTest ? Layers.QUADS_LAYER : Layers.OVERLAY_QUADS_LAYER;
            VertexConsumer quadsVertexConsumer = renderContext.consumers().getBuffer(layer);

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
        int a = color.getAlpha();


        consumer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        consumer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }

    private static class Layers {

        private static final RenderPipeline.Snippet RENDERTYPE_OVERLAY_LINES_SNIPPET = RenderPipeline.builder()
                .withUniform("Globals", UniformType.UNIFORM_BUFFER)
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withVertexShader("core/rendertype_lines")
                .withFragmentShader("core/rendertype_lines")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.LINES)
                .buildSnippet();

        private static final RenderPipeline OVERLAY_LINES = RenderPipelines.register(
                RenderPipeline.builder(RENDERTYPE_OVERLAY_LINES_SNIPPET)
                        .withLocation("pipeline/lines")
                        .build()
        );

        private static final RenderLayer.MultiPhase OVERLAY_LINES_LAYER = RenderLayer.MultiPhase.of(
                "overlay_lines",
                1536,
                OVERLAY_LINES,
                RenderLayer.MultiPhaseParameters.builder()
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.0D)))
                        .layering(VIEW_OFFSET_Z_LAYERING)
                        .target(ITEM_ENTITY_TARGET)
                        .build(false)
        );

        private static final RenderPipeline LINES = RenderPipelines.register(
                RenderPipeline.builder(RENDERTYPE_OVERLAY_LINES_SNIPPET)
                        .withLocation("pipeline/lines")
                        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                        .build()
        );

        private static final RenderLayer.MultiPhase LINES_LAYER = RenderLayer.MultiPhase.of(
                "overlay_lines",
                1536,
                LINES,
                RenderLayer.MultiPhaseParameters.builder()
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.0D)))
                        .layering(VIEW_OFFSET_Z_LAYERING)
                        .target(ITEM_ENTITY_TARGET)
                        .build(false)
        );

        private static final RenderPipeline.Snippet OVERLAY_QUADS_SNIPPET = RenderPipeline.builder()
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                .buildSnippet();

        private static final RenderPipeline OVERLAY_QUADS = RenderPipelines.register(
                RenderPipeline.builder(OVERLAY_QUADS_SNIPPET)
                        .withLocation("pipeline/debug_quads")
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

        private static final RenderPipeline QUADS = RenderPipelines.register(
                RenderPipeline.builder(OVERLAY_QUADS_SNIPPET)
                        .withLocation("pipeline/debug_quads")
                        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                        .build()
        );

        private static final RenderLayer.MultiPhase QUADS_LAYER = RenderLayer.of(
                "overlay_quads",
                1536,
                false,
                true,
                QUADS,
                RenderLayer.MultiPhaseParameters.builder()
                        .target(ITEM_ENTITY_TARGET)
                        .layering(VIEW_OFFSET_Z_LAYERING)
                        .build(false)
        );

    }

}
