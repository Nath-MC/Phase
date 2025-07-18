package com.purpynaxx.phase.settings;

import net.minecraft.util.math.BlockPos;

public class PositionInputSetting extends Setting<BlockPos> {

    private PositionInputSetting(Builder builder) {
        super(builder);
    }

    public static class Builder extends Setting.Builder<Builder> {

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public PositionInputSetting build() {
            check();
            return new PositionInputSetting(this);
        }

    }

}
