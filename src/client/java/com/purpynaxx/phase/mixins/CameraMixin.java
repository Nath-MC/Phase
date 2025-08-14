package com.purpynaxx.phase.mixins;

import com.purpynaxx.phase.helpers.player.Rotations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Unique
    private static final Rotations rotations = Rotations.getInstance();

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void overrideRotation(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (focusedEntity instanceof ClientPlayerEntity) {
            if (rotations.getShouldOverride()) {
                float yaw = rotations.getYaw();
                float pitch = rotations.getPitch();
                setRotation(yaw, pitch);
            }
        }
    }

    @Shadow
    public abstract void setRotation(float yaw, float pitch);

}
