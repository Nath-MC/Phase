package com.purpynaxx.phase.settings;

import com.mojang.serialization.Codec;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(Builder builder) {
        super(builder);
    }

    public void toggle() {
        boolean initialState = this.getValue();
        this.setValue(!initialState);
    }

    @Override
    public Codec<Boolean> getCodec() {
        return Codec.BOOL;
    }

    public static class Builder extends Setting.Builder<BooleanSetting.Builder> {

        @Override
        public BooleanSetting.Builder self() {
            return this;
        }

        @Override
        public BooleanSetting build() {
            return new BooleanSetting(this);
        }

    }

}
