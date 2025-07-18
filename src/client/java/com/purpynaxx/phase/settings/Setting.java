package com.purpynaxx.phase.settings;

import net.minecraft.text.Text;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class Setting<T> {

    private final String id;
    private final Text name;
    private final Text description;
    private final Supplier<T> defaultValueSupplier;
    private T value;

    @SuppressWarnings("unchecked")
    protected Setting(Builder<?> builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description.orElse(Text.empty());
        this.defaultValueSupplier = (Supplier<T>) builder.defaultValue;
        this.value = this.defaultValueSupplier.get();
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

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Setting value cannot be null");
        }
        this.value = value;
    }

    public boolean isDefault() {
        return value.equals(defaultValueSupplier);
    }

    public void reset() {
        this.value = this.defaultValueSupplier.get();
    }

    protected static abstract class Builder<T extends Builder<T>> {

        protected String id;
        protected Text name;
        protected Optional<Text> description;
        protected Supplier<?> defaultValue;

        public T id(String id) {
            this.id = id;
            return self();
        }

        public T name(Text name) {
            this.name = name;
            return self();
        }

        public T description(Text description) {
            this.description = Optional.of(description);
            return self();
        }

        public T defaultValue(Supplier<?> defaultValue) {
            this.defaultValue = defaultValue;
            return self();
        }

        protected void check() {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("Setting ID cannot be null or empty");
            } else if (name == null) {
                throw new IllegalArgumentException("Setting name cannot be null");
            } else if (defaultValue == null) {
                throw new IllegalArgumentException("Default value must be provided");
            }
        }

        protected abstract T self();

        public abstract Setting<?> build();

    }

}
