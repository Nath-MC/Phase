package com.purpynaxx.phase.helpers.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an abstract step in a path. Each node defines a specific action
 * to be executed and a cost associated with that action.
 */
public abstract class Node {

    protected static final MinecraftClient client = MinecraftClient.getInstance();

    protected final BlockPos pos;
    protected final int gCost; // Distance from starting node
    protected final int hCost; // Heuristic distance to target node
    protected final int fCost; // gCost + hCost
    protected final @Nullable Node previousNode;

    public Node(BlockPos pos, int gCost, int hCost, @Nullable Node previousNode) {
        this.pos = pos;
        this.gCost = gCost;
        this.hCost = hCost;
        this.fCost = gCost + hCost;
        this.previousNode = previousNode;
    }

    /**
     * Executes the action associated with this node.
     */
    public abstract void execute();

    /**
     * This method states whether a node should continue {@link Node#execute() executing} or not.
     * <br>
     * If {@code true} is returned, the action that this node was performing is likely to be done, ready to execute the next one.
     *
     * @return {@code true} if the executor should continue to the next node.
     */
    public abstract boolean isDone();

    /**
     * Gets the cost of performing the action of this node.
     *
     * @return The cost.
     */
    public abstract int getCost();

    public BlockPos getPos() {
        return pos;
    }

    public int getFCost() {
        return fCost;
    }

    public int getGCost() {
        return gCost;
    }

    public @Nullable Node getPreviousNode() {
        return previousNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return pos.equals(node.pos);
    }

}
