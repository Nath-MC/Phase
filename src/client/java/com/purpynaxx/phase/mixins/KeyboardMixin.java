package com.purpynaxx.phase.mixins;

import com.purpynaxx.phase.helpers.input.InputUtils;
import net.minecraft.client.Keyboard;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Redirect(method = "onKey(JIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;setKeyPressed(Lnet/minecraft/client/util/InputUtil$Key;Z)V"))
    private void cancelKeyPress(InputUtil.Key key, boolean pressed) {
        if (InputUtils.shouldAllowKeyPress(key))
            KeyBinding.setKeyPressed(key, pressed);
    }

}
