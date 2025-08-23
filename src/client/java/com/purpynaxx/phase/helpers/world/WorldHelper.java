package com.purpynaxx.phase.helpers.world;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class WorldHelper {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private WorldHelper() {}

    public static BlockPos getNearestLoaded(BlockPos target) {

        if (client.world == null) {
            throw new IllegalStateException();
        }

        BlockPos current = client.player.getBlockPos();

        if (isLoaded(target)) {
            return target; // The chunk the goal is in is already loaded, no need to find the nearest loaded goal.
        }

        Vec3d currentVec = new Vec3d(current.getX(), current.getY(), current.getZ());
        Vec3d goalVec = new Vec3d(target.getX(), target.getY(), target.getZ());

        Vec3d direction = goalVec.subtract(currentVec).normalize();
        double distance = current.getChebyshevDistance(target);

        BlockPos lastLoadedPos = current;

        // Iterate linearly from the current position to the goal, searching for the last loaded position.
        for (double i = 0; i < distance; i++) {
            Vec3d nextPosVec = currentVec.add(direction.multiply(i));
            BlockPos nextPos = BlockPos.ofFloored(nextPosVec);

            if (isLoaded(nextPos)) {
                lastLoadedPos = nextPos;
            } else break; // The position is not loaded, break and return the last known position.
        }

        return lastLoadedPos;
    }

    public static boolean isLoaded(BlockPos pos) {
        return client.world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * Returns a list of all the 26 <a href="https://en.wikipedia.org/wiki/Moore_neighborhood">Moore neighbors</a> of the given position.
     *
     * @param pos The position.
     */
    public static List<BlockPos> getMooreNeighbors(BlockPos pos) {

        if (client.world == null) {
            throw new IllegalStateException();
        }

        List<BlockPos> neighbors = new ArrayList<>(26);

        for (int x = -1; x <= 1; x++) {

            for (int y = -1; y <= 1; y++) {

                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) continue; // skip the given pos

                    if (!client.world.getWorldBorder().contains(pos))
                        continue; // ignore neighbors outside the world border

                    BlockPos blockPos = pos.add(x, y, z);

                    if (!isLoaded(blockPos)) continue; // ignore neighbors that are not loaded

                    neighbors.add(blockPos);

                }

            }

        }

        return neighbors;
    }

    /**
     * Checks if a block is passable for movement.
     * A block is passable if it's not solid and the block below it is solid.
     *
     * @param pos The position to check.
     * @return True if the block is passable.
     */
    public static boolean isPassable(BlockPos pos) {
        if (client.world == null) return false;
        BlockState head = client.world.getBlockState(pos);
        BlockState feet = client.world.getBlockState(pos);
        BlockState ground = client.world.getBlockState(pos.down());
        return !head.isSolid() && !feet.isSolid() && ground.isSolid();
    }

}
