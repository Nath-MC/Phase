package com.purpynaxx.phase.mixins;

import com.purpynaxx.phase.helpers.player.Rotations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private static final Rotations rotations = Rotations.getInstance();

    @Inject(method = "changeLookDirection(DD)V", at = @At(value = "HEAD"), cancellable = true)
    private void redirectRotationChanges(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity) {
            if (rotations.getShouldOverride()) {
                rotations.onMouseMove(cursorDeltaX, cursorDeltaY);
                ci.cancel();
            }
        }
    }

}
