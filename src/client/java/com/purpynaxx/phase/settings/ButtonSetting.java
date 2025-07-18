package com.purpynaxx.phase.settings;

public class ButtonSetting extends Setting<Runnable> {

    public ButtonSetting(Builder builder) {
        super(builder);
    }

    public void onPress() {
        getValue().run();
    }

    public static class Builder extends Setting.Builder<ButtonSetting.Builder> {

        @Override
        public ButtonSetting.Builder self() {
            return this;
        }

        @Override
        public ButtonSetting build() {
            check();
            return new ButtonSetting(this);
        }

        public ButtonSetting.Builder defaultValue(Runnable defaultValue) {
            this.defaultValue = () -> defaultValue;
            return this;
        }

    }

}
