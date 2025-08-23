package com.purpynaxx.phase.helpers.pathfinding;

import com.purpynaxx.phase.helpers.chat.ChatHelper;
import com.purpynaxx.phase.helpers.input.InputUtils;
import com.purpynaxx.phase.helpers.player.PlayerHelper;
import com.purpynaxx.phase.render.Renderable;
import com.purpynaxx.phase.render.Renderer;
import com.purpynaxx.phase.render.impl.PathLine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


public class PathExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger("Phase/PathExecutor");
    private static final Color PATH_COLOR = new Color(0, 0, 255, 100);

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Renderer renderer = Renderer.getInstance();
    private static final AtomicBoolean isCalculating = new AtomicBoolean();

    @Nullable
    private static PathExecutor currentPathExecutor;

    @Nullable
    private static CompletableFuture<Optional<List<Node>>> currentTask;
    private static long lastCalculationStartTime = 0;

    private final Stack<Renderable> pathRenderables = new Stack<>();

    private Pathfinder pathfinder;
    private List<Node> path;
    private int currentNodeIndex;

    private PathExecutor(List<Node> path, Pathfinder pathfinder) {
        setPath(path, pathfinder);
    }

    public static void stop() {
        boolean wasCalculating = false;
        if (currentTask != null) {
            currentTask.cancel(true);
            wasCalculating = true;
        }

        boolean wasExecuting = false;
        if (currentPathExecutor != null) {
            currentPathExecutor.clear();
            wasExecuting = true;
        }

        if (wasCalculating && wasExecuting)
            ChatHelper.send("§cPath calculation and execution cancelled.");
        else if (wasCalculating)
            ChatHelper.send("§cPath calculation cancelled.");
        else if (wasExecuting)
            ChatHelper.send("§cPath execution cancelled.");
        else
            ChatHelper.send("§cNothing to cancel.");
    }

    public static void tick() {
        if (currentPathExecutor == null) return;

        if (client.player == null) {
            currentPathExecutor.clear();
            return;
        }
        currentPathExecutor.onTick();
    }

    public static Optional<PathExecutor> getCurrentPathExecutor() {
        return Optional.ofNullable(currentPathExecutor);
    }

    public static void findAndExecutePath(BlockPos start, BlockPos end) {
        long now = System.currentTimeMillis();
        if (now - lastCalculationStartTime < (long) 1000) {
            return;
        }

        if (isCalculating.getAndSet(true)) {
            LOGGER.warn("Already calculating a path...");
            return;
        }

        lastCalculationStartTime = now;

        Pathfinder pathfinder = new Pathfinder(start, end);
        currentTask = pathfinder.findPathAsync();

        currentTask.whenCompleteAsync((pathOptional, throwable) -> {
            float timeTaken = (System.currentTimeMillis() - lastCalculationStartTime) / 1000f;

            if (throwable != null) {
                handlePathfindingError(throwable);
            } else if (pathOptional.isPresent() && !pathOptional.get().isEmpty()) {
                handlePathfindingSuccess(pathOptional.get(), pathfinder);
            } else {
                ChatHelper.send(String.format("§cCould not find a path. (%.1fs, %d nodes checked)", timeTaken, pathfinder.getComputedNodes()));
                if (currentPathExecutor != null) currentPathExecutor.clear();
            }

            isCalculating.set(false);
            currentTask = null;
        }, client);
    }

    private static void handlePathfindingSuccess(List<Node> newPath, Pathfinder pathfinder) {
        if (currentPathExecutor == null) {
            currentPathExecutor = new PathExecutor(newPath, pathfinder);
            ChatHelper.send(String.format("§aPath found with %d steps!", newPath.size()));
        } else
            currentPathExecutor.setPath(newPath, pathfinder);
    }

    private static void handlePathfindingError(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();

        if (cause instanceof CancellationException) return; // Do nothing, stop() will handle it

        ChatHelper.send(String.format("§cError during path calculation: %s", cause.getClass().getSimpleName()));
        LOGGER.error("An exception occurred during path calculation:", throwable);

        if (currentPathExecutor != null) {
            currentPathExecutor.clear();
        }
    }

    private void clear() {
        PlayerHelper.resetInputs();
        renderer.clear(this);
        pathRenderables.clear();
        InputUtils.setAllowMovementKeys(true);
        currentPathExecutor = null;
    }

    private void onTick() {
        if (currentNodeIndex >= path.size()) {
            onGoalReached();
            return;
        }

        Node currentNode = path.get(currentNodeIndex);

        if (currentNode.isDone()) {
            if (++currentNodeIndex >= path.size()) {
                return;
            }

            currentNode = path.get(currentNodeIndex);

            if (!pathRenderables.isEmpty() && currentNodeIndex > 3) {
                Renderable removed = pathRenderables.pop();
                renderer.remove(this, removed);
            }
        }

        currentNode.execute();
    }

    private void onGoalReached() {
        ChatHelper.send("§aPath reached !");
        clear();
    }

    public void onChunkLoaded() {
        if (client.player != null) {
            findAndExecutePath(client.player.getBlockPos(), this.pathfinder.getEnd());
        }
    }

    private void setPath(List<Node> newPath, Pathfinder newPathfinder) {
        renderer.clear(this);
        this.pathRenderables.clear();

        this.path = newPath;
        this.pathfinder = newPathfinder;
        this.currentNodeIndex = 0;

        if (path.size() > 1) {
            Vec3d lastNodePos = null;
            for (Node node : path.reversed()) {
                Vec3d currentNodePos = node.getPos().toCenterPos();
                if (lastNodePos != null) {
                    pathRenderables.push(new PathLine.Builder()
                            .start(currentNodePos)
                            .end(lastNodePos)
                            .color(PATH_COLOR)
                            .build());
                }
                lastNodePos = currentNodePos;
            }
        }
        renderer.addAll(this, pathRenderables);

        InputUtils.setAllowMovementKeys(false);
    }

}
