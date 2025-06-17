package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.settings.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class BooleanWidget extends SettingWidget<Boolean> {

    public static final int BORDER_COLOR = Color.DARK_GRAY.getRGB();
    private static final int TOGGLE_WIDTH = 20;
    private static final int TOGGLE_HEIGHT = 10;
    private static final int TEXT_COLOR = Color.WHITE.getRGB();
    private static final int INACTIVE_COLOR = new Color(255, 100, 100, 200).getRGB();
    private static final int ACTIVE_COLOR = new Color(100, 255, 100, 200).getRGB();

    public BooleanWidget(BooleanSetting setting, int x, int y, int width) {
        super(setting, x, y, width);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        String name = setting.getName();
        context.drawText(textRenderer, name, x + 5, y + (height - textRenderer.fontHeight) / 2, TEXT_COLOR, false);

        int toggleX = x + width - TOGGLE_WIDTH - 5;
        int toggleY = y + (height - TOGGLE_HEIGHT) / 2;

        context.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, new Color(50, 50, 50, 200).getRGB());

        if (setting.getValue()) {
            context.fill(toggleX + TOGGLE_WIDTH / 2, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, ACTIVE_COLOR);
            context.drawBorder(toggleX + TOGGLE_WIDTH / 2, toggleY, TOGGLE_WIDTH / 2, TOGGLE_HEIGHT, BORDER_COLOR);
        } else {
            context.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH / 2, toggleY + TOGGLE_HEIGHT, INACTIVE_COLOR);
            context.drawBorder(toggleX, toggleY, TOGGLE_WIDTH / 2, TOGGLE_HEIGHT, BORDER_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
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