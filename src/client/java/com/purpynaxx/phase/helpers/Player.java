package com.purpynaxx.phase.helpers;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class with helper methods for player-related operations.
 */
public class Player {

    /**
     * Sets the player's rotation (yaw and pitch) to face a specific Vec3d.
     *
     * @param player     The player object
     * @param position   The position to face
     * @param serverside If true, the rotation will only take place server side.
     */
    public static void lookAt(ClientPlayerEntity player, Vec3d position, boolean serverside) {
        if (player == null) return;
        Vec3d eyePos = player.getEyePos();

        double diffX = position.x - eyePos.x;
        double diffY = position.y - eyePos.y;
        double diffZ = position.z - eyePos.z;

        double horizontalDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontalDistance));

        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -90.0F, 90.0F);

        if (serverside) {
            PlayerMoveC2SPacket.LookAndOnGround packet = new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision);
            player.networkHandler.sendPacket(packet);
        } else {
            player.setYaw(yaw);
            player.setPitch(pitch);
        }
    }

    /**
     * Updates the player's position to the specified coordinates.
     *
     * @param player     The player object
     * @param pos        The target position
     * @param serverside If true, the position update will only take place on the server side.
     */
    public static void setPosition(ClientPlayerEntity player, Vec3d pos, boolean serverside) {
        if (player == null) return;
        if (player.getPos().equals(pos)) return;

        if (serverside) {
            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(pos, player.isOnGround(), player.horizontalCollision);
            player.networkHandler.sendPacket(packet);
        } else {
            player.setPos(pos.x, pos.y, pos.z);
        }
    }

    /**
     * Synchronizes the player's current position with the server.
     *
     * @param player The player object
     */
    public static void syncPosition(ClientPlayerEntity player) {
        if (player == null) return;
        Vec3d pos = player.getPos();
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos, player.isOnGround(), player.horizontalCollision));
    }

    public static boolean isInSurvival(ClientPlayerEntity player) {
        if (player == null) return false;
        return player.getGameMode().isSurvivalLike();
    }
}