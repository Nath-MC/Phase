package com.purpynaxx.phase.helpers.pathfinding;

import com.purpynaxx.phase.helpers.pathfinding.nodes.MovementNode;
import com.purpynaxx.phase.helpers.world.WorldHelper;
import com.purpynaxx.phase.render.Renderer;
import com.purpynaxx.phase.render.impl.Line;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class Pathfinder implements Supplier<Optional<List<Node>>> {

    private static final Renderer renderer = Renderer.getInstance();

    private final BlockPos start;
    private final BlockPos end;

    private final PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(Node::getFCost));
    private final HashSet<BlockPos> closedSet = new HashSet<>();

    private BlockPos currentGoal;
    private Node lastBestNode;

    private int computedNodes;

    public Pathfinder(BlockPos start, BlockPos end) {
        this.start = start;
        this.end = end;
    }

    public BlockPos getEnd() {
        return end;
    }

    /**
     * Asynchronously finds a path from the start to the end position.
     *
     * @return A CompletableFuture that will contain the list of nodes, or an empty optional if no path is found.
     */
    public CompletableFuture<Optional<List<Node>>> findPathAsync() {
        return CompletableFuture.supplyAsync(this);
    }

    /**
     * The core A* pathfinding logic.
     * This is executed by the CompletableFuture.
     */
    @Override
    public Optional<List<Node>> get() {

        // First we assert that the player is in a world.
        if (MinecraftClient.getInstance().player == null) {
            return Optional.empty();
        }

        // Second, we assert that the start position of the path is loaded.
        if (!WorldHelper.isLoaded(start)) {
            throw new IllegalStateException("The start position is not loaded.");
        }

        // Retrieve, the current end, which might or not be the final end.
        currentGoal = WorldHelper.getNearestLoaded(end);

        // Start node
        openSet.add(new MovementNode(start, 0, start.getManhattanDistance(currentGoal), null));
        computedNodes++;

        while (!openSet.isEmpty()) {

            Node currentNode = openSet.poll(); // Retrieve the node with the lowest fCost.

            if (lastBestNode != null) {
                addRenderable(lastBestNode, currentNode);
            }

            if (currentNode.getPos().equals(currentGoal)) {
                // The end has been reached !
                // Let's build the final path from the start to the current node.

                Node node = currentNode;
                List<Node> path = new LinkedList<>();
                renderer.clear(this);

                while (node != null) {
                    path.addFirst(node);
                    node = node.getPreviousNode();
                }

                return Optional.of(Collections.unmodifiableList(path));

            }

            // We are about to compute this node, so we add its position to the set to ensure it won't be processed again.
            closedSet.add(currentNode.getPos());

            // Then we discover and compute all nodes in the Moore neighborhood, but in 3 dimensions
            for (BlockPos neighborPos : WorldHelper.getMooreNeighbors(currentNode.getPos())) {
                computedNodes++;

                if (closedSet.contains(neighborPos)) continue;
                if (!WorldHelper.isPassable(neighborPos)) continue;

                int tentativeGCost = currentNode.getGCost() + 1; // TODO when custom nodes will be implemented, change this using Node#getCost

                // This block of code will retrieve all nodes that share the same position as the current neighbor node.
                // It will only keep the one with the lowest fCost.
                // If a node is retrieved, it means this node was previously part of another path, but we found a shorter path to the end using this node.
                // We then remove the old node object, and add the new one.
                // If it is not, then we can simply add it to the priority queue.
                openSet.stream()
                       .filter(node -> node.getPos().equals(neighborPos))
                       .findFirst().
                       ifPresentOrElse(node -> {
                           if (tentativeGCost < node.getGCost()) {
                               // This new path to the neighbor is better. Update the node.
                               openSet.remove(node);
                               int newHCost = neighborPos.getManhattanDistance(currentGoal);
                               Node betterNode = new MovementNode(neighborPos, tentativeGCost, newHCost, currentNode); // TODO determine the needed node type
                               openSet.add(betterNode);
                           }
                       }, () -> {
                           int newHCost = neighborPos.getManhattanDistance(currentGoal);
                           Node neighborNode = new MovementNode(neighborPos, tentativeGCost, newHCost, currentNode); // TODO determine the needed node type
                           openSet.add(neighborNode);
                       });
            }
            lastBestNode = currentNode;
        }

        // If this point is reached, A* checked every possible node and didn't reach the end.
        renderer.clear(this);
        return Optional.empty();
    }

    private void addRenderable(Node node1, Node node2) {
        Vec3d start = node1.getPos().toBottomCenterPos();
        Vec3d end = node2.getPos().toBottomCenterPos();
        Line line = new Line.Builder().start(start).end(end).color(Color.WHITE).debug(true).build();
        renderer.addRenderable(this, line);
    }

    public int getComputedNodes() {
        return computedNodes;
    }

}
