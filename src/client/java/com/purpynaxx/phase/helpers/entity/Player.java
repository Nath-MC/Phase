package com.purpynaxx.phase.helpers.entity;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class with helper methods for player-related operations.
 */
public final class Player {

    /**
     * Sets the player's rotation (yaw and pitch) to face a specific Vec3d.
     *
     * @param player   The player object
     * @param position The position to face
     * @param side     The side to apply the rotation to (client, server, or both)
     */
    public static void lookAt(ClientPlayerEntity player, Vec3d position, Side side) {
        if (player == null) return;
        Vec3d eyePos = player.getEyePos();

        double diffX = position.x - eyePos.x;
        double diffY = position.y - eyePos.y;
        double diffZ = position.z - eyePos.z;

        double horizontalDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontalDistance));

        setRotation(player, yaw, pitch, side);
    }

    /**
     * Updates the player's position to the specified coordinates.
     *
     * @param player The player object
     * @param pos    The target position
     * @param side   The side to apply the position update to (client, server, or both)
     */
    public static void setPosition(ClientPlayerEntity player, Vec3d pos, Side side) {
        if (player == null) return;
        if (player.getPos().equals(pos)) return;

        if (side != Side.SERVER) { // side == CLIENT || side == BOTH
            player.setPosition(pos);
            refresh(player);
        }

        if (side != Side.CLIENT) { // side == SERVER || side == BOTH
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos, player.isOnGround(), player.horizontalCollision));
        }
    }

    /**
     * Sets the player's rotation (yaw and pitch) to the specified values.
     *
     * @param player The player object
     * @param yaw    The target yaw angle
     * @param pitch  The target pitch angle
     * @param side   The side to apply the rotation to (client, server, or both)
     */
    public static void setRotation(ClientPlayerEntity player, float yaw, float pitch, Side side) {
        if (player == null) return;

        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -90.0F, 90.0F);

        if (side != Side.SERVER) { // side == CLIENT || side == BOTH
            player.setYaw(yaw);
            player.setPitch(pitch);
            refresh(player);
        }

        if (side != Side.CLIENT) { // side == SERVER || side == BOTH
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
        }
    }

    /**
     * Synchronizes the player's current position with the server.
     *
     * @param player The player object
     */
    public static void syncPosition(ClientPlayerEntity player) {
        if (player == null) return;
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getPos(), player.isOnGround(), player.horizontalCollision));
    }

    /**
     * Synchronizes the player's current rotation (yaw and pitch) with the server.
     *
     * @param player The player object
     */
    public static void syncRotation(ClientPlayerEntity player) {
        if (player == null) return;

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
    }

    /**
     * Synchronizes the player's full position and rotation with the server.
     *
     * @param player The player object
     */
    public static void syncFull(ClientPlayerEntity player) {
        if (player == null) return;

        Vec3d pos = player.getPos();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        boolean onGround = player.isOnGround();
        boolean horizontalCollision = player.horizontalCollision;

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(pos, yaw, pitch, onGround, horizontalCollision));
    }

    public static boolean canTakeFallDamage(ClientPlayerEntity player) {
        if (player == null) return false;

        return (player.getGameMode().isSurvivalLike())
                && !player.isSleeping()
                && !player.isGliding()
                && !player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !player.hasStatusEffect(StatusEffects.LEVITATION)
                && !player.isInvulnerable()
                && player.fallDistance > 3.0F
                && player.getVelocity().y < 0;
    }

    private static void refresh(ClientPlayerEntity player) {
        Vec3d pos = player.getPos();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        player.refreshPositionAndAngles(pos, yaw, pitch);
    }

    public enum Side {
        CLIENT,
        SERVER,
        BOTH
    }

}