package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.settings.ListSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ListWidget extends SettingWidget<Integer> {

    public ListWidget(ListSetting<?> setting, int x, int y) {
        super(setting, x, y);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        String value = ((ListSetting<?>) setting).get().toString();
        String displayText = setting.getName() + ":";
        int textY = y + (height - textRenderer.fontHeight) / 2;

        context.drawText(textRenderer, Text.literal(displayText), x + 5, textY, -1, false);
        context.drawText(textRenderer, Text.literal(value), x + width - 5 - textRenderer.getWidth(value), textY, -1, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            ((ListSetting<?>) setting).cycle();
            return true;
        }
        return false;
    }

}