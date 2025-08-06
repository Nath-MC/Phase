package com.purpynaxx.phase.mixins;

import com.purpynaxx.phase.helpers.player.PlayerHelper;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.modules.movements.NoRotation;
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
    private static final Modules modules = Modules.getInstance();

    @Unique
    private static boolean aBoolean = true;

    @Inject(method = "update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void overrideRotation(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (modules.isModuleActive(NoRotation.class) && focusedEntity instanceof ClientPlayerEntity) {
            if (aBoolean) {
                aBoolean = false;
                PlayerHelper.resetCameraRotation();
            }

            float yaw = PlayerHelper.getCameraYaw();
            float pitch = PlayerHelper.getCameraPitch();
            setRotation(yaw, pitch);
        } else if (focusedEntity instanceof ClientPlayerEntity) aBoolean = true;
    }

    @Shadow
    public abstract void setRotation(float yaw, float pitch);

}
