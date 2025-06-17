package com.purpynaxx.phase.settings;

import org.jetbrains.annotations.NotNull;

public abstract class Setting<T> {

    private final String name;
    private final String description;
    private @NotNull
    final T defaultValue;
    private @NotNull T value;

    public Setting(String name, String description, @NotNull T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public Class<T> getType() {
        return (Class<T>) this.value.getClass();
    }

    public String getDescription() {
        return description;
    }

    public @NotNull T getDefaultValue() {
        return defaultValue;
    }

    public @NotNull T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value == null ? this.defaultValue : value;
    }

    public void resetValue() {
        this.value = this.defaultValue;
    }
}
