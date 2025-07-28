package com.purpynaxx.phase.settings;

import java.util.List;
import java.util.Optional;

public class ListSetting<T> extends Setting<Integer> {

    private final List<T> values;

    private ListSetting(Builder<T> builder) {
        super(builder);
        this.values = builder.values.orElseThrow(() -> new IllegalArgumentException("Values cannot be null"));

        if (this.values.isEmpty()) {
            throw new IllegalArgumentException("Values list cannot be empty");
        } else if (((int) builder.defaultValue.get()) < 0 || ((int) builder.defaultValue.get()) >= this.values.size()) {
            int index = ((int) builder.defaultValue.get());
            String message = String.format("The provided default value index %d is out of bounds for range [0, %d]", index, this.values.size() - 1);
            throw new IndexOutOfBoundsException(message);
        }
    }

    public void cycle() {
        this.setValue((value + 1) % values.size());
    }

    public T get() {
        return values.get(value);
    }

    public static class Builder<T> extends Setting.Builder<ListSetting.Builder<T>> {

        private Optional<List<T>> values = Optional.empty();

        public Builder<T> values(List<T> values) {
            this.values = Optional.of(values);
            return this;
        }

        @Override
        public Builder<T> self() {
            return this;
        }

        @Override
        public ListSetting<T> build() {
            if (this.defaultValue.isEmpty()) this.defaultValue = Optional.of(0);
            return new ListSetting<>(this);
        }

    }

}