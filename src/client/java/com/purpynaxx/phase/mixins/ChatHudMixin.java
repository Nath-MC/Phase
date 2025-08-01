package com.purpynaxx.phase.mixins;

import com.purpynaxx.phase.helpers.chat.ChatHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(at = @At("HEAD"), method = "logChatMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", cancellable = true)
    private void cancelChatMessageLog(ChatHudLine message, CallbackInfo ci) {
        if (message.content().getString().startsWith(ChatHelper.PREFIX))
            ci.cancel();
    }

}
