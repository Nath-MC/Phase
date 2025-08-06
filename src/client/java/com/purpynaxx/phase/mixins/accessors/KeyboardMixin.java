package com.purpynaxx.phase.mixins.accessors;

import com.purpynaxx.phase.helpers.pathfinding.PathExecutor;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Redirect(method = "onKey(JIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;setKeyPressed(Lnet/minecraft/client/util/InputUtil$Key;Z)V"))
    private void cancelKeyPress(InputUtil.Key key, boolean pressed) {
        if (PathExecutor.getCurrentPathExecutor().isPresent()) {

            int code = key.getCode();
            KeyBinding forward = client.options.forwardKey;
            KeyBinding backward = client.options.backKey;
            KeyBinding left = client.options.leftKey;
            KeyBinding right = client.options.rightKey;
            KeyBinding jump = client.options.jumpKey;
            KeyBinding sneak = client.options.sneakKey;

            // run on dreams and hope, but hey, it works
            if (forward.matchesKey(code, 0) || backward.matchesKey(code, 0)
                        || left.matchesKey(code, 0) || right.matchesKey(code, 0)
                        || jump.matchesKey(code, 0) || sneak.matchesKey(code, 0)) {
                return;
            }

        }
        KeyBinding.setKeyPressed(key, pressed);
    }

}
