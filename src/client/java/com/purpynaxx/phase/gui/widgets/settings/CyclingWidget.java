package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.settings.CyclingSetting;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.awt.*;

public class CyclingWidget extends SettingWidget<Enum<?>> {

    @SuppressWarnings("unchecked")
    public CyclingWidget(CyclingSetting<?> setting, int x, int y, int width) {
        super((Setting<Enum<?>>) setting, x, y, width);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        String enumValue = setting.getValue().toString();
        String displayText = setting.getName() + ":";
        int textY = y + (height - textRenderer.fontHeight) / 2;

        context.drawText(textRenderer, Text.literal(displayText), x + 5, textY, Color.WHITE.getRGB(), false);
        context.drawText(textRenderer, Text.literal(enumValue), x + width - 5 - textRenderer.getWidth(enumValue), textY, Color.WHITE.getRGB(), false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) { // Left click
            if (setting instanceof CyclingSetting<?> cyclingSetting) {
                cyclingSetting.onClick();
                return true;
            }
        }
        return false;
    }

}