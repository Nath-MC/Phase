package com.purpynaxx.phase.mixin.accessors;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleScreen.class)
public interface TitleScreenMixin {

    @Accessor
    boolean getDoBackgroundFade();

    @Accessor
    void setDoBackgroundFade(boolean doBackGroundFade);
}
