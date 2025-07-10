package com.purpynaxx.phase.gui.widgets.settings;

import com.purpynaxx.phase.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;

public abstract class SettingWidget<T> implements Element, Drawable {

    protected static final MinecraftClient client = MinecraftClient.getInstance();
    protected static final TextRenderer textRenderer = client.textRenderer;

    protected static final int DEFAULT_WIDTH = 100;
    protected static final int DEFAULT_HEIGHT = 20;

    protected final Setting<T> setting;
    protected final int width;
    protected final int height;

    protected int x;
    protected int y;

    public SettingWidget(Setting<T> setting, int x, int y) {
        this.setting = setting;
        this.x = x;
        this.y = y;
        this.width = DEFAULT_WIDTH;
        this.height = DEFAULT_HEIGHT;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

}