package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.interfaces.client.ClientTick;
import com.purpynaxx.phase.helpers.entity.Player;
import com.purpynaxx.phase.modules.impl.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AirJump extends Module implements ClientTick.AFTER {

    private static final Text description = Text.translatable("modules.movements.airjump.description");
    private boolean wasPressed = false;

    private AirJump() {
        super(description);
    }

    @Override
    public void afterClientTick(MinecraftClient client) {
        if (client.player.groundCollision || client.player.isOnGround() || client.player.isSubmergedInWater()) return;

        KeyBinding jumpKey = client.options.jumpKey;

        if (jumpKey.wasPressed() && client.player.getVelocity().y < -0.1f) {
            wasPressed = true;
            jumpKey.reset();

            Vec3d position = client.player.getPos();
            double y = Math.floor(position.y);

            Player.setPosition(client.player, new Vec3d(position.x, y, position.z), Player.Side.SERVER);
            client.player.jump();
        } else if (wasPressed) {
            wasPressed = false;
            jumpKey.reset();
        }
    }

}
