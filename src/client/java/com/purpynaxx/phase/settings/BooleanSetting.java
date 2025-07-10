package com.purpynaxx.phase.settings;

import net.minecraft.text.Text;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String id, Text name, Text description, boolean defaultValue) {
        super(id, name, description, defaultValue);
    }

    public void toggle() {
        boolean initialState = this.getValue();
        this.setValue(!initialState);
    }

}