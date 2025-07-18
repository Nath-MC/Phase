package com.purpynaxx.phase.settings;

import java.util.List;
import java.util.function.Supplier;

public class CyclingSetting<T extends Enum<?>> extends Setting<T> {

    private final List<T> values;
    private int currentIndex;

    private CyclingSetting(Builder<T> builder) {
        super(builder);
        this.values = builder.values;
    }

    public void onClick() {
        int nextIndex = (currentIndex + 1) % values.size();
        this.setValue(this.values.get(nextIndex));
        this.currentIndex = nextIndex;
    }

    public static class Builder<T extends Enum<?>> extends Setting.Builder<CyclingSetting.Builder<T>> {

        private List<T> values;

        @Override
        public Builder<T> defaultValue(Supplier<?> defaultValue) {
            return this;
        }

        @Override
        protected void check() {
            super.check();
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException("Values cannot be null or empty");
            }
        }

        public Builder<T> values(List<T> values) {
            this.values = values;
            return this;
        }

        @Override
        public Builder<T> self() {
            return this;
        }

        @Override
        public CyclingSetting<T> build() {
            this.defaultValue = () -> values.getFirst();
            check();
            return new CyclingSetting<>(this);
        }

    }

}