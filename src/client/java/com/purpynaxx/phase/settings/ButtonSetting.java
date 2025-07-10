package com.purpynaxx.phase.settings;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ButtonSetting extends Setting<Runnable> {

    public ButtonSetting(String id, Text name, Text description, @NotNull Runnable defaultValue) {
        super(id, name, description, defaultValue);
    }

    public void onPress() {
        getValue().run();
    }

}
