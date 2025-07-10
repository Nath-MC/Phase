package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.helpers.color.ColorHelper;
import com.purpynaxx.phase.settings.BooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;


public class BooleanWidget extends SettingWidget<Boolean> {

    private static final int TOGGLE_WIDTH = 20;
    private static final int TOGGLE_HEIGHT = 10;

    private static final int BG_COLOR = 0xFF323232;                 // new Color(50, 50, 50, 255).getRGB()
    private static final int BORDER_COLOR_INACTIVE = 0xFF505050;    // new Color(80, 80, 80, 255).getRGB()
    private static final int TEXT_COLOR = 0xFFFFFFFF;               // new Color(255, 255, 255, 255).getRGB()
    private static final int INACTIVE_COLOR = 0xFFFF3C3C;           // new Color(255, 60, 60, 255).getRGB()
    private static final int ACTIVE_COLOR = 0xFF3CFF3C;             // new Color(60, 255, 60, 200).getRGB()

    private static final int ANIMATION_DURATION = 100;

    private long lastToggleTime = -1;

    public BooleanWidget(BooleanSetting setting, int x, int y) {
        super(setting, x, y);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        String name = setting.getName();
        context.drawText(textRenderer, name, x + 5, y + (height - textRenderer.fontHeight) / 2, TEXT_COLOR, false);

        int toggleX = x + width - TOGGLE_WIDTH - 5;
        int toggleY = y + (height - TOGGLE_HEIGHT) / 2;

        context.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, BG_COLOR);


        float animationProgress = 1.0f;
        if (lastToggleTime != -1) {
            long elapsedTime = System.currentTimeMillis() - lastToggleTime;
            animationProgress = (float) elapsedTime / ANIMATION_DURATION;
            animationProgress = MathHelper.clamp(animationProgress, 0.0f, 1.0f);

            if (animationProgress >= 1.0f) {
                lastToggleTime = -1;
            }
        }

        int interpolatedX;
        int interpolatedColor;

        if (setting.getValue()) {
            interpolatedX = MathHelper.lerp(animationProgress, toggleX, toggleX + TOGGLE_WIDTH / 2);
            interpolatedColor = ColorHelper.interpolateColor(INACTIVE_COLOR, ACTIVE_COLOR, animationProgress);
        } else {
            interpolatedX = MathHelper.lerp(animationProgress, toggleX + TOGGLE_WIDTH / 2, toggleX);
            interpolatedColor = ColorHelper.interpolateColor(ACTIVE_COLOR, INACTIVE_COLOR, animationProgress);
        }

        context.fill(interpolatedX, toggleY, interpolatedX + TOGGLE_WIDTH / 2, toggleY + TOGGLE_HEIGHT, interpolatedColor);
        context.drawBorder(toggleX - 1, toggleY - 1, TOGGLE_WIDTH + 2, TOGGLE_HEIGHT + 2, BORDER_COLOR_INACTIVE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            lastToggleTime = System.currentTimeMillis();
            ((BooleanSetting) setting).toggle();
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            int toggleX = x + width - TOGGLE_WIDTH - 5;
            int toggleY = y + (height - TOGGLE_HEIGHT) / 2;

            return mouseX >= toggleX && mouseX <= toggleX + TOGGLE_WIDTH && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT;
        }
        return false;
    }

}
