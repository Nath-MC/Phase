package com.purpynaxx.phase.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class BooleanSetting extends Setting<Boolean> {

    // isn't useful currently, but will be in the future
    public static final Codec<BooleanSetting> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(BooleanSetting::getName),
            Codec.STRING.fieldOf("description").forGetter(BooleanSetting::getDescription),
            Codec.BOOL.fieldOf("defaultValue").forGetter(BooleanSetting::getDefaultValue),
            Codec.BOOL.fieldOf("value").forGetter(BooleanSetting::getValue)
    ).apply(instance, (name, description, defaultValue, value) -> {
        BooleanSetting setting = new BooleanSetting(name, description, defaultValue);
        setting.setValue(value);
        return setting;
    }));

    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() {
        boolean initialState = this.getValue();
        this.setValue(!initialState);
    }
}