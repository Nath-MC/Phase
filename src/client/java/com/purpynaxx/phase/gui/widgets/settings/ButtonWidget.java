package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.helpers.color.ColorHelper;
import com.purpynaxx.phase.settings.ButtonSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;


public class ButtonWidget extends SettingWidget<Runnable> {

    private static final int BUTTON_COLOR_BASE = 0xFF1E1E1E;
    private static final int BUTTON_COLOR_HOVER = 0xFF333333;
    private static final int BORDER_COLOR = 0xFF505050;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int PADDING = 2;

    private final Text buttonText;
    private final AnimationState hoverAnimation = new AnimationState();


    public ButtonWidget(ButtonSetting setting, int x, int y) {
        super(setting, x, y);
        this.buttonText = Text.literal(setting.getName());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update hover animation state
        boolean mouseOver = isMouseOver(mouseX, mouseY);
        float multiplier = mouseOver ? AnimationState.HOVER_ANIMATION_MULTIPLIER : AnimationState.HOVER_DECAY_MULTIPLIER;
        hoverAnimation.update(mouseOver, delta, AnimationState.ANIMATION_SPEED, multiplier);

        float hoverProgress = hoverAnimation.getValue();

        // Interpolate background color based on hover state
        int backgroundColor = ColorHelper.interpolateColor(BUTTON_COLOR_BASE, BUTTON_COLOR_HOVER, hoverProgress);

        int x = this.x + PADDING;
        int width = this.width - PADDING * 2;
        int y = this.y + PADDING + 1;
        int height = this.height - PADDING * 2 - 1;

        // Draw background and border
        context.fill(x, y, x + width, y + height, backgroundColor);
        context.drawBorder(x, y, width, height, BORDER_COLOR);

        // Draw button text
        int textX = x + (width - textRenderer.getWidth(buttonText)) / 2;
        int textY = y + (height - textRenderer.fontHeight) / 2 + 1;
        context.drawText(textRenderer, buttonText, textX, textY, TEXT_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) { // Left click
            ((ButtonSetting) setting).onPress();
            return true;
        }
        return false;
    }


    /**
     * Helper class for animation state.
     */
    private static class AnimationState {

        static final float ANIMATION_SPEED = 0.1f;
        static final float HOVER_ANIMATION_MULTIPLIER = 5.0f;
        static final float HOVER_DECAY_MULTIPLIER = 3.0f;


        private float value = 0.0f;

        public float getValue() {
            return value;
        }

        public void update(boolean increasing, float delta, float speed, float multiplier) {
            if (increasing) {
                value += delta * speed * multiplier;
            } else {
                value -= delta * speed * multiplier;
            }
            value = MathHelper.clamp(value, 0.0f, 1.0f);
        }

    }

}
