package com.purpynaxx.phase.mixins;

import com.purpynaxx.phase.helpers.player.PlayerHelper;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.modules.movements.NoRotation;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public class MouseMixin {

    @Unique
    private static final Modules modules = Modules.getInstance();

    @Redirect(method = "updateMouse(D)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void cancel(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        if (modules.isModuleActive(NoRotation.class)) {
            float yawDelta = (float) cursorDeltaX * 0.15f;
            float pitchDelta = (float) cursorDeltaY * 0.15f;
            float yaw = PlayerHelper.getCameraYaw() + yawDelta;
            float pitch = Math.clamp(PlayerHelper.getCameraPitch() + pitchDelta, -90, 90);
            PlayerHelper.setCameraRotation(yaw, pitch);
            return;
        }

        instance.changeLookDirection(cursorDeltaX, cursorDeltaY);
    }

}
