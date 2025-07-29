package com.purpynaxx.phase.settings;

import com.mojang.serialization.Codec;
import com.purpynaxx.phase.modules.Module;
import net.minecraft.text.Text;

import java.util.Optional;

public abstract class Setting<T> {

    protected final String id;
    protected final Text name;
    protected final Text description;
    protected final Module module;
    protected final T defaultValue;
    protected T value;

    @SuppressWarnings("unchecked")
    protected Setting(Builder<?> builder) {
        this.id = builder.id.orElseThrow(() -> new IllegalArgumentException("Setting ID cannot be null"));
        this.name = builder.name.orElseThrow(() -> new IllegalArgumentException("Setting name cannot be null"));
        this.description = builder.description.orElse(Text.empty());
        this.module = builder.module.orElseThrow(() -> new IllegalArgumentException("Module cannot be null"));
        this.defaultValue = (T) builder.defaultValue.orElseThrow(() -> new IllegalArgumentException("A default value must be provided"));
        this.value = this.defaultValue;
    }

    public Module getModule() {
        return module;
    }

    public abstract Codec<T> getCodec();

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

    public String getDescription() {
        return description.getString();
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Setting value cannot be null");
        }
        this.value = value;
    }

    public void castAndSetValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Setting value cannot be null");
        }

        if (!getType().isInstance(value)) {
            throw new ClassCastException(String.format("Cannot cast %s to %s", value.getClass().getName(), getType().getName()));
        }
        this.value = getType().cast(value);
    }

    public boolean isDefault() {
        return value.equals(defaultValue);
    }

    public void reset() {
        this.value = this.defaultValue;
    }

    protected static abstract class Builder<B extends Builder<B>> {

        protected Optional<String> id = Optional.empty();
        protected Optional<Text> name = Optional.empty();
        protected Optional<Text> description = Optional.empty();
        protected Optional<Module> module = Optional.empty();
        protected Optional<Object> defaultValue = Optional.empty();

        public B id(String id) {
            if (id.isBlank()) throw new IllegalArgumentException("Setting ID cannot be empty or blank");

            this.id = Optional.of(id);
            return self();
        }

        protected abstract B self();

        public B name(Text name) {
            this.name = Optional.of(name);
            return self();
        }

        public B description(Text description) {
            this.description = Optional.of(description);
            return self();
        }

        public B module(Module module) {
            this.module = Optional.of(module);
            return self();
        }

        public B defaultValue(Object defaultValue) {
            this.defaultValue = Optional.of(defaultValue);
            return self();
        }

        public abstract Setting<?> build();

    }

}
