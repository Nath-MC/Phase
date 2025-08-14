package com.purpynaxx.phase.helpers.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;

import static com.purpynaxx.phase.Phase.LOGGER;

public class Rotations {

    private static final Rotations INSTANCE = new Rotations();
    private static MinecraftClient client;

    private boolean clearOnNextTick = false;
    private float yaw, pitch;
    private boolean shouldOverride;

    private Rotations() {}

    public static Rotations getInstance() {
        client = MinecraftClient.getInstance();
        return INSTANCE;
    }

    public static float getYaw(Position position) {
        Vec3d eyePos = client.player.getEyePos();
        return (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(position.getZ() - eyePos.z, position.getX() - eyePos.x)) - 90.0F);
    }

    public static float getPitch(Position position) {
        Vec3d eyePos = client.player.getEyePos();
        double deltaX = position.getX() - eyePos.x;
        double deltaZ = position.getZ() - eyePos.z;
        return (float) -Math.toDegrees(Math.atan2(position.getY() - eyePos.y, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean getShouldOverride() {
        return shouldOverride;
    }

    public void tick() {
        if (clearOnNextTick) {
            clearOnNextTick = false;
        } else if (shouldOverride) {
            attach();
        }
    }

    private void attach() {
        if (shouldOverride) {
            shouldOverride = false;
            client.player.setYaw(yaw);
            client.player.setPitch(pitch);
        }
    }

    public void onMouseMove(double cursorDeltaX, double cursorDeltaY) {
        if (!shouldOverride) return;
        yaw += (float) cursorDeltaX * 0.15f;
        pitch += (float) cursorDeltaY * 0.15f;
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);
        yaw = MathHelper.wrapDegrees(yaw);
    }

    public void submit(float yaw, float pitch, Runnable task) {
        if (!shouldOverride) {
            detach();
        }

        PlayerHelper.setRotation(yaw, pitch, PlayerHelper.Side.BOTH);

        try {
            task.run();
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
        }

        clearOnNextTick = true;
    }

    private void detach() {
        if (!shouldOverride) {
            shouldOverride = true;
            yaw = client.player.getYaw();
            pitch = client.player.getPitch();
        }
    }

}
