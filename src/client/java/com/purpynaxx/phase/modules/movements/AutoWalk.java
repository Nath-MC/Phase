package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.interfaces.client.ClientTick;
import com.purpynaxx.phase.events.interfaces.world.WorldChunkEvent;
import com.purpynaxx.phase.helpers.entity.Player;
import com.purpynaxx.phase.helpers.render.DrawMode;
import com.purpynaxx.phase.helpers.render.Renderer;
import com.purpynaxx.phase.helpers.render.impl.FaceOverlay;
import com.purpynaxx.phase.modules.Module;
import com.purpynaxx.phase.settings.ButtonSetting;
import com.purpynaxx.phase.settings.PositionInputSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoWalk extends Module implements ClientTick.AFTER, WorldChunkEvent.LOAD {

    private static final Text description = Text.translatable("modules.movements.autowalk.description");

    private final ExecutorService pathExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isPathfinding = new AtomicBoolean(false);

    private final PositionInputSetting goalSetting = new PositionInputSetting.Builder()
            .id("goal")
            .name(Text.translatable("modules.movements.autowalk.goal.name"))
            .description(Text.translatable("modules.movements.autowalk.goal.description"))
            .defaultValue(() -> BlockPos.ORIGIN)
            .build();

    private final ButtonSetting startPathfindingButton = new ButtonSetting.Builder()
            .id("start_pathfinding")
            .name(Text.literal("Start Pathfinding"))
            .description(Text.literal("Click to set the goal and start pathfinding"))
            .defaultValue(this::onStart)
            .build();

    private BlockPos goal;
    private @Nullable List<PathNode> currentPath;

    private AutoWalk() {
        super(description);
        registerSettings(goalSetting, startPathfindingButton);
    }

    private static boolean isPassable(BlockState state) {
        return state.getCollisionShape(client.world, BlockPos.ORIGIN).isEmpty()
                && IsNotFluid(state);
    }

    private static boolean IsNotFluid(BlockState state) {
        return !state.getFluidState().isIn(FluidTags.WATER)
                && !state.getFluidState().isIn(FluidTags.LAVA);
    }

    @Override
    public void onChunkLoad(ClientWorld world, WorldChunk chunk) {
        onWorldChange();
    }

    private void onStart() {
        BlockPos userDefinedGoal = goalSetting.getValue();
        setGoal(userDefinedGoal);
        this.setActive(true);
    }

    public void setGoal(BlockPos goal) {
        this.goal = goal;
        clearPath();
        recalculatePath();
    }

    @Override
    public void afterClientTick(MinecraftClient client) {
        if (currentPath == null || currentPath.isEmpty()) return;

        PathNode nextNode = currentPath.getFirst();
        BlockPos playerPos = client.player.getBlockPos();
        BlockPos nextPos = nextNode.pos;

        if (playerPos.equals(nextPos) || isPlayerCloseTo(client.player.getPos(), nextPos)) {
            currentPath.removeFirst();
            if (currentPath.isEmpty()) {
                // We reached the goal !
                onEnd(true);
                return;
            }
            nextNode = currentPath.getFirst();
            nextPos = nextNode.pos;
        }

        handleMovement(nextPos);

        for (PathNode node : currentPath) {
            FaceOverlay faceOverlay = new FaceOverlay(node.pos.down(), Direction.UP, new Color(0, 0, 255, 75), DrawMode.FILL, 2, true, true);
            renderer.addRenderable(faceOverlay);
        }
    }

    @Override
    protected void onActivate() {
        if (goalSetting.isDefault()) this.toggle();
    }

    private void onEnd(boolean success) {
        clearPath();
        goalSetting.reset();
        goal = null;

        if (success) {
            client.player.sendMessage(Text.translatable("modules.movements.autowalk.goal_reached").formatted(Formatting.GREEN), true);
        } else {
            client.player.sendMessage(Text.translatable("modules.movements.autowalk.goal_failed").formatted(Formatting.RED), true);
        }

        this.setActive(false);
    }

    private void handleMovement(BlockPos targetPos) {

        Vec3d position = Vec3d.ofCenter(targetPos);
        Vec3d eyePos = client.player.getEyePos();

        double diffX = position.x - eyePos.x;
        double diffZ = position.z - eyePos.z;

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = 0f;

        Player.setRotation(client.player, yaw, pitch, Player.Side.BOTH);

        client.options.forwardKey.setPressed(true);
        client.options.sprintKey.setPressed(true);

        if (targetPos.getY() > client.player.getBlockPos().getY() && client.player.isOnGround()) {
            client.player.jump();
        }
    }

    public void onWorldChange() {
        if (this.goal != null) {
            recalculatePath();
        }
    }

    private void recalculatePath() {
        if (isPathfinding.getAndSet(true)) {
            return;
        }

        if (this.goal == null) {
            isPathfinding.set(false);
            return;
        }

        final BlockPos startPos = client.player.getBlockPos();
        final BlockPos currentGoal = this.goal;

        CompletableFuture.supplyAsync(() -> findPath(startPos, currentGoal), pathExecutor)
                .thenAccept(path -> {
                    this.currentPath = path;
                    isPathfinding.set(false);

                    if (currentPath == null) {
                        onEnd(false);
                    }
                });
    }

    private List<PathNode> findPath(BlockPos start, BlockPos end) {
        ClientWorld world = client.world;
        BlockPos finalGoal = findReachableGoal(world, start, end);

        // A* Algorithm Implementation
        // The 'openSet' is a priority queue of nodes to be evaluated, starting with our start position.
        // Nodes are prioritized by their F-cost (distance from start + estimated distance to goal).
        // This ensures we always explore the most promising node first.
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));

        // The 'closedSet' stores nodes that have already been evaluated, to avoid redundant checks.
        // Using a HashSet provides fast (O(1)) lookups.
        Set<BlockPos> closedSet = new HashSet<>();

        // Create the starting node and add it to the open set.
        PathNode startNode = new PathNode(start, null, 0, getHeuristic(start, finalGoal));
        openSet.add(startNode);

        // --- Main A* Loop ---
        // The loop continues as long as there are nodes to evaluate in the open set.
        while (!openSet.isEmpty()) {
            // Poll the node with the lowest F-cost from the priority queue. This is our current position.
            PathNode currentNode = openSet.poll();

            // If the current node is the goal, we have found our path.
            if (currentNode.pos.equals(finalGoal)) {
                return reconstructPath(currentNode); // Build the path backwards from the goal.
            }

            // Add the current node's position to the closed set so we don't process it again.
            closedSet.add(currentNode.pos);

            // --- Neighbor Exploration ---
            // Now, we check all valid neighbors of the current node.
            for (BlockPos neighborPos : getNeighbors(world, currentNode.pos)) {
                // If we have already evaluated this neighbor, skip it.
                if (closedSet.contains(neighborPos)) {
                    continue;
                }

                // Calculate the cost to move from the start to this neighbor through the current node.
                double tentativeGCost = currentNode.gCost + getDistance(currentNode.pos, neighborPos);

                // Check if we've found a better path to this neighbor.
                PathNode neighborNode = new PathNode(neighborPos, currentNode, tentativeGCost, getHeuristic(neighborPos, finalGoal));

                // If the neighbor is not in the open set, add it.
                // If it is already in the open set but this new path is shorter, the priority queue will handle it.
                // (A more robust implementation would find and update the existing node, but this is simpler and often sufficient).
                if (openSet.stream().noneMatch(n -> n.pos.equals(neighborPos) && n.gCost < tentativeGCost)) {
                    openSet.removeIf(n -> n.pos.equals(neighborPos)); // Remove old, more expensive path to neighbor
                    openSet.add(neighborNode);
                }
            }
        }
        // If the open set becomes empty, and we haven't reached the goal, no path was established.
        return null;
    }

    private List<PathNode> reconstructPath(PathNode goalNode) {
        List<PathNode> path = new ArrayList<>();
        PathNode current = goalNode;
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);
        if (!path.isEmpty()) {
            path.removeFirst();
        }
        return path;
    }

    private BlockPos findReachableGoal(ClientWorld world, BlockPos start, BlockPos goal) {
        // We trace the path from start to goal to find the last valid point in a loaded chunk.
        // This is more reliable than just checking the goal's chunk initially.
        Vec3d startVec = Vec3d.ofCenter(start);
        Vec3d goalVec = Vec3d.ofCenter(goal);

        // If start and goal are the same, no need to trace.
        if (start.equals(goal)) {
            return goal;
        }

        Vec3d direction = goalVec.subtract(startVec).normalize();
        double distance = start.getManhattanDistance(goal);

        BlockPos lastLoadedPos = start;

        // Iterate along the line from start to goal, one block at a time.
        for (int i = 1; i < distance; i++) {
            Vec3d nextVec = startVec.add(direction.multiply(i));
            BlockPos nextPos = BlockPos.ofFloored(nextVec);

            // If we find a chunk that is not loaded along the path...
            if (!world.getChunkManager().isChunkLoaded(nextPos.getX() >> 4, nextPos.getZ() >> 4)) {
                // ...then our temporary goal is the last known loaded position.
                // We should try to find a walkable block near this last position to make it a valid target for A*.
                return findNearestWalkable(world, lastLoadedPos);
            }
            lastLoadedPos = nextPos;
        }

        // If the entire path to the goal is within loaded chunks, the goal is reachable.
        // We still find the nearest walkable block to ensure the goal itself is a valid standing position.
        return findNearestWalkable(world, goal);
    }

    // Helper method to find a nearby walkable block, searching downwards then upwards.
    private BlockPos findNearestWalkable(ClientWorld world, BlockPos pos) {
        // Search downwards from the given position to find solid ground.
        for (int y = pos.getY(); y >= world.getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (isWalkable(world, checkPos)) {
                return checkPos;
            }
        }
        // If no ground is found below, search upwards as a fallback.
        for (int y = pos.getY() + 1; y < world.getTopYInclusive(); y++) {
            BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
            if (isWalkable(world, checkPos)) {
                return checkPos;
            }
        }
        // If absolutely nothing is found, return the original position.
        // A* will have to figure it out from there.
        return pos;
    }

    private List<BlockPos> getNeighbors(ClientWorld world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;

                BlockPos neighbor = pos.add(x, 0, z);

                // Check for straight or diagonal movement on the same level
                if (isWalkable(world, neighbor)) {
                    // For diagonal movement, ensure path is not obstructed
                    if (x != 0 && z != 0) {
                        if (isPassable(world.getBlockState(pos.add(x, 0, 0))) && isPassable(world.getBlockState(pos.add(0, 0, z)))) {
                            neighbors.add(neighbor);
                        }
                    } else {
                        neighbors.add(neighbor);
                    }
                }

                // Check for ascending
                else if (isWalkable(world, neighbor.up()) && isPassable(world.getBlockState(pos.up(2)))) {
                    neighbors.add(neighbor.up());
                }

                // Check for descending
                else if (isWalkable(world, neighbor.down())) {
                    neighbors.add(neighbor.down());
                }
            }
        }
        return neighbors;
    }

    private boolean isWalkable(ClientWorld world, BlockPos pos) {
        BlockState supportingBlock = world.getBlockState(pos.down());
        if (isPassable(supportingBlock) && !IsNotFluid(supportingBlock)) {
            return false;
        }

        BlockState footLevelBlock = world.getBlockState(pos);
        if (footLevelBlock.getBlock() instanceof DoorBlock) {
            if (!footLevelBlock.get(DoorBlock.OPEN)) {
                return false;
            }
        } else if (!isPassable(footLevelBlock)) {
            return false;
        }

        BlockState headLevelBlock = world.getBlockState(pos.up());
        if (headLevelBlock.getBlock() instanceof DoorBlock) {
            return headLevelBlock.get(DoorBlock.OPEN);
        } else return isPassable(headLevelBlock);
    }

    @Override
    public void onDeactivate() {
        clearPath();
        renderer.removeRenderablesIf(Renderer::isDebug);
    }

    private void clearPath() {
        if (this.currentPath != null) {
            this.currentPath.clear();
        }
        if (client.options.forwardKey.isPressed()) {
            client.options.forwardKey.setPressed(false);
        }
    }

    private boolean isPlayerCloseTo(Vec3d playerPos, BlockPos targetPos) {
        return playerPos.isInRange(Vec3d.ofCenter(targetPos), 0.5);
    }

    // --- A* Cost Functions ---
    // Heuristic: An estimate of the distance from a node to the goal.
    // We use squared Euclidean distance for efficiency (avoids slow square root calculations).
    private double getHeuristic(BlockPos a, BlockPos b) {
        return a.getSquaredDistance(b);
    }

    // G-Cost: The actual distance traveled from the start node to the current node.
    private double getDistance(BlockPos a, BlockPos b) {
        return a.getSquaredDistance(b);
    }

    // --- Nested Class for A* Nodes ---
    private static class PathNode {

        public final BlockPos pos;
        public final PathNode parent;
        public final double gCost; // Cost from start to this node
        public final double hCost; // Heuristic cost from this node to goal
        public final double fCost; // gCost + hCost

        public PathNode(BlockPos pos, PathNode parent, double gCost, double hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

    }

}
