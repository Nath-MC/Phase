package com.purpynaxx.phase.settings;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public abstract class Setting<T> {

    private final String id;
    private final Text name;
    private final Text description;
    private final @NotNull T defaultValue;
    private @NotNull T value;

    public Setting(String id, Text name, Text description, @NotNull T defaultValue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultValue = this.value = defaultValue;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name.getString();
    }

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) this.value.getClass();
    }

    public Text getDescription() {
        return description;
    }

    public @NotNull T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value == null ? this.defaultValue : value;
    }

    public boolean isDefault() {
        return value.equals(defaultValue);
    }

    public void reset() {
        this.value = this.defaultValue;
    }

}
