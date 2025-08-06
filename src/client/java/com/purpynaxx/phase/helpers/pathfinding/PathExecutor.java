package com.purpynaxx.phase.helpers.pathfinding;

import com.purpynaxx.phase.helpers.chat.ChatHelper;
import com.purpynaxx.phase.helpers.player.PlayerHelper;
import com.purpynaxx.phase.render.Renderable;
import com.purpynaxx.phase.render.Renderer;
import com.purpynaxx.phase.render.impl.Line;
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
import java.util.concurrent.atomic.AtomicLong;

public class PathExecutor {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger("Phase/PathExecutor");
    private static final Renderer renderer = Renderer.getInstance();

    private static final AtomicBoolean calculating = new AtomicBoolean();
    private static final AtomicLong lastCalculationTime = new AtomicLong();

    private static @Nullable PathExecutor currentPathExecutor;
    private static CompletableFuture<Optional<List<Node>>> task;

    private final Stack<Renderable> renderables = new Stack<>();
    private final long startTime;

    private Pathfinder pathfinder;
    private List<Node> path;
    private int currentNodeIndex;

    private PathExecutor(List<Node> path, Pathfinder pathfinder) {
        this.startTime = System.currentTimeMillis();
        setPath(path, pathfinder);
    }

    private void setPath(List<Node> newPath, Pathfinder newPathfinder) {
        renderer.clear(this);
        this.renderables.clear();
        this.path = newPath;
        this.pathfinder = newPathfinder;
        this.currentNodeIndex = 0;

        Vec3d lastNodePos = null;
        for (Node node : path.reversed()) {
            Vec3d currentNodePos = node.getPos().toBottomCenterPos();
            if (lastNodePos != null) {
                Line line = new Line.Builder().start(currentNodePos).end(lastNodePos).color(Color.GREEN).build();
                renderables.push(line);
            }
            lastNodePos = currentNodePos;
        }
        renderer.addAll(this, renderables);
    }

    public static void tick() {
        if (currentPathExecutor == null) return;
        if (client.player == null) {
            currentPathExecutor.clear();
            return;
        }
        currentPathExecutor.onTick();
    }

    private void onTick() {
        if (path == null || path.isEmpty() || currentNodeIndex >= path.size()) {
            onGoalReached();
            return;
        }

        Node currentNode = path.get(currentNodeIndex);

        if (currentNode.isDone()) {
            currentNodeIndex++;
            if (!renderables.isEmpty()) {
                Renderable removed = renderables.pop();
                renderer.remove(this, removed);
            }
            return;
        }
        currentNode.execute();
    }

    private void onGoalReached() {
        long timeTaken = System.currentTimeMillis() - startTime;
        ChatHelper.send(String.format("§cPath reached in %.3fs !", timeTaken / 1000f));
        clear();
    }

    private void clear() {
        PlayerHelper.resetInputs();
        renderer.clear(this);
        renderables.clear();
        currentPathExecutor = null;
    }

    public static Optional<PathExecutor> getCurrentPathExecutor() {
        return Optional.ofNullable(currentPathExecutor);
    }

    public static void stop() {
        boolean calculating = PathExecutor.calculating.getAndSet(false);
        boolean executing = currentPathExecutor != null;

        if (calculating) {
            task.cancel(false);
            ChatHelper.send("§cPath calculation has been cancelled.");
        }

        if (executing) {
            currentPathExecutor.clear();
            ChatHelper.send("§cPath execution has been cancelled.");
        }

        if (!executing && !calculating)
            ChatHelper.send("§cNothing has been canceled.");
    }

    public void onChunkLoaded() {
        findAndExecutePath(client.player.getBlockPos(), this.pathfinder.getEnd());
    }

    /**
     * The main entry point for finding and executing a path.
     * Can be used for both initial pathfinding and recalculation.
     */
    public static void findAndExecutePath(BlockPos start, BlockPos end) {
        if (calculating.getAndSet(true)) {
            return;
        }

        long current = System.currentTimeMillis();
        if ((current - lastCalculationTime.get()) / 1000f > 1)
            lastCalculationTime.set(current);
        else return;


        Pathfinder pathfinder = new Pathfinder(start, end);
        task = pathfinder.findPathAsync();

        task.whenCompleteAsync((pathOptional, throwable) -> {

            float timeTaken = (System.currentTimeMillis() - current) / 1000f;

            if (throwable != null) {
                Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();

                if (throwable instanceof CancellationException) return;

                ChatHelper.send(String.format("§cAn error occurred during path calculation. (%s)", cause.getClass().getSimpleName()));
                LOGGER.error("An exception occurred during path calculation:", throwable);

                if (currentPathExecutor != null)
                    currentPathExecutor.clear();
            } else if (pathOptional != null && pathOptional.isPresent() && !pathOptional.get().isEmpty()) {

                List<Node> newPath = pathOptional.get();

                if (currentPathExecutor == null) {
                    currentPathExecutor = new PathExecutor(newPath, pathfinder);
                    ChatHelper.send(String.format("§aPath found in %.3fs with %d steps!", timeTaken, newPath.size()));
                } else {
                    currentPathExecutor.setPath(newPath, pathfinder);
                    ChatHelper.send(String.format("Path updated in %.3fs.", timeTaken));
                }

            } else {
                ChatHelper.send(String.format("§cCould not find a path to the destination. (%.1fs for %d nodes taken in account)", timeTaken, pathfinder.getComputedNodes()));
                if (currentPathExecutor != null) currentPathExecutor.clear();
            }

            calculating.set(false);
        }, client);
    }

}
