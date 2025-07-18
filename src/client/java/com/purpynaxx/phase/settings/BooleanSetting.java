package com.purpynaxx.phase.settings;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(Builder builder) {
        super(builder);
    }

    public void toggle() {
        boolean initialState = this.getValue();
        this.setValue(!initialState);
    }

    public static class Builder extends Setting.Builder<BooleanSetting.Builder> {

        @Override
        public BooleanSetting.Builder self() {
            return this;
        }

        @Override
        public BooleanSetting build() {
            check();
            return new BooleanSetting(this);
        }

    }

}