package com.purpynaxx.phase.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() {
        boolean initialState = this.getValue();
        this.setValue(!initialState);
    }
}