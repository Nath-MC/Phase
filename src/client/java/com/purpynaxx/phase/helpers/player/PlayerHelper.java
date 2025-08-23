package com.purpynaxx.phase.helpers.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Utility class with helper methods for player-related operations.
 */
public final class PlayerHelper {

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static ClientPlayerEntity player;

    /**
     * Sets the player's rotation (yaw and pitch) to face a specific Vec3d.
     *
     * @param position The position to face
     * @param side     The side to apply the rotation to (client, server, or both)
     */
    public static void lookAt(Vec3d position, Side side) {
        updatePlayer();
        Vec3d eyePos = player.getEyePos();

        double diffX = position.x - eyePos.x;
        double diffY = position.y - eyePos.y;
        double diffZ = position.z - eyePos.z;

        double horizontalDistance = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontalDistance));

        setRotation(yaw, pitch, side);
    }

    private static void updatePlayer() {
        if (player != client.player) player = client.player;
        if (player == null) throw new IllegalStateException();
    }

    /**
     * Sets the player's rotation (yaw and pitch) to the specified values.
     *
     * @param yaw    The target yaw angle
     * @param pitch  The target pitch angle
     * @param side   The side to apply the rotation to (client, server, or both)
     */
    public static void setRotation(float yaw, float pitch, Side side) {
        updatePlayer();

        yaw = MathHelper.wrapDegrees(yaw);
        pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -90.0F, 90.0F);

        if (side != Side.SERVER) { // side == CLIENT || side == BOTH
            player.setYaw(yaw);
            player.setPitch(pitch);
            refresh();
        }

        if (side != Side.CLIENT) { // side == SERVER || side == BOTH
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
        }
    }

    private static void refresh() {
        updatePlayer();
        Vec3d pos = player.getPos();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        player.refreshPositionAndAngles(pos, yaw, pitch);
    }

    /**
     * Sets the player's rotation (yaw) to face a specific Vec3d without changing the pitch.
     *
     * @param position The position to face
     * @param side     The side to apply the rotation to (client, server, or both)
     * @param pitch    The pitch angle to maintain
     */
    public static void lookAtNoPitch(Vec3d position, Side side, float pitch) {
        updatePlayer();
        Vec3d eyePos = player.getEyePos();

        double diffX = position.x - eyePos.x;
        double diffZ = position.z - eyePos.z;

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;

        setRotation(yaw, pitch, side);
    }

    /**
     * Updates the player's position to the specified coordinates.
     *
     * @param pos  The target position
     * @param side The side to apply the position update to (client, server, or both)
     */
    public static void setPosition(Vec3d pos, Side side) {
        updatePlayer();
        if (player.getPos().equals(pos)) return;

        if (side != Side.SERVER) { // side == CLIENT || side == BOTH
            player.setPosition(pos);
            refresh();
        }

        if (side != Side.CLIENT) { // side == SERVER || side == BOTH
            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos, player.isOnGround(), player.horizontalCollision));
        }
    }

    /**
     * Synchronizes the player's current position with the server.
     *
     */
    public static void syncPosition() {
        updatePlayer();
        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(player.getPos(), player.isOnGround(), player.horizontalCollision));
    }

    /**
     * Synchronizes the player's current rotation (yaw and pitch) with the server.
     *
     */
    public static void syncRotation() {
        updatePlayer();

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
    }

    /**
     * Synchronizes the player's full position and rotation with the server.
     *
     */
    public static void syncFull() {
        updatePlayer();
        Vec3d pos = player.getPos();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        boolean onGround = player.isOnGround();
        boolean horizontalCollision = player.horizontalCollision;

        player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(pos, yaw, pitch, onGround, horizontalCollision));
    }

    public static boolean canTakeFallDamage() {
        updatePlayer();
        return (player.getGameMode().isSurvivalLike())
                && !player.isSleeping()
                && !player.isGliding()
                && !player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !player.hasStatusEffect(StatusEffects.LEVITATION)
                && !player.isInvulnerable()
                && player.fallDistance > 3.0F
                && player.getVelocity().y < 0;
    }

    public static void resetInputs() {
        updatePlayer();
        client.options.forwardKey.reset();
        client.options.backKey.reset();
        client.options.rightKey.reset();
        client.options.leftKey.reset();
        client.options.jumpKey.reset();
        client.options.sneakKey.reset();
    }

    public enum Side {
        CLIENT,
        SERVER,
        BOTH
    }

}
