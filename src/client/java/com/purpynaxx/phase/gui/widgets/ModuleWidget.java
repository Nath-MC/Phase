package com.purpynaxx.phase.gui.widgets;

import com.purpynaxx.phase.modules.impl.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ModuleWidget implements Element, Drawable {

    private static final float ANIMATION_SPEED = 0.1f;

    private static final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private final Module module;

    private final Text message;

    private final int width;
    private final int height;

    private final TooltipWidget tooltipWidget;

    private float hoverAnimationTimer = 0.0f;
    private float tooltipTimer = 0.0f;

    private boolean hovered;

    private int x;
    private int y;

    public ModuleWidget(Module module, int x, int y, int width, int height) {
        this.module = module;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = Text.of(module.getName());
        this.tooltipWidget = module.getDescription() != null && !module.getDescription().isEmpty() ? new TooltipWidget(module.getDescription()) : null;
    }

    public Text getMessage() {
        return message;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

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

    private boolean isActive() {
        return module.isActive();
    }

    private void setActive(boolean active) {
        module.setActive(active);
    }

    public boolean hasTooltip() {
        return tooltipWidget != null;
    }

    public boolean isTooltipReady() {
        return tooltipTimer >= 1.0f;
    }

    public void renderTooltip(DrawContext context, int mouseX, int mouseY, float delta) {
        if (tooltipWidget != null && tooltipTimer >= 1.0f) {
            tooltipWidget.refreshPos(this.x, this.y, 108, 8, context.getScaledWindowWidth(), context.getScaledWindowHeight());
            tooltipWidget.render(context, mouseX, mouseY, delta);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, boolean renderTooltips) {
        update(delta);

        int baseAlpha = 0;
        int hoverAlpha = 60;
        int currentAlpha = MathHelper.lerp(hoverAnimationTimer, baseAlpha, hoverAlpha);

        if (this.isActive()) {
            int color1 = new Color(255, 255, 255, 40).getRGB();
            int color2 = new Color(255, 255, 255, 70).getRGB();
            int backgroundColor = interpolateColor(color1, color2, hoverAnimationTimer);
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), backgroundColor);
        } else if (this.isActive())
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), new Color(255, 255, 255, 40).getRGB());
        else if (currentAlpha > 0)
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), new Color(255, 255, 255, currentAlpha).getRGB());

        Text message = this.getMessage();
        int textX = this.getX() + this.getWidth() / 2 - textRenderer.getWidth(message) / 2;
        int textY = this.getY() + (this.getHeight() - textRenderer.fontHeight) / 2 + 1;
        int textColor = this.isActive() ? Color.WHITE.getRGB() : interpolateColor(Color.LIGHT_GRAY.getRGB(), Color.WHITE.getRGB(), hoverAnimationTimer);
        context.drawText(textRenderer, message, textX, textY, textColor, false);

        if (renderTooltips) renderTooltip(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(context, mouseX, mouseY, delta, true);
    }

    private void update(float delta) {
        if (this.isHovered()) {
            hoverAnimationTimer += delta * ANIMATION_SPEED * 5;
            tooltipTimer += delta * ANIMATION_SPEED;
        } else {
            hoverAnimationTimer -= delta * ANIMATION_SPEED * 3;
            tooltipTimer = 0;
        }

        hoverAnimationTimer = Math.clamp(hoverAnimationTimer, 0.0f, 1.0f);
        tooltipTimer = Math.clamp(tooltipTimer, 0.0f, 1.0f);
    }

    private int interpolateColor(int color1, int color2, float progress) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = MathHelper.lerp(progress, a1, a2);
        int r = MathHelper.lerp(progress, r1, r2);
        int g = MathHelper.lerp(progress, g1, g2);
        int b = MathHelper.lerp(progress, b1, b2);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void onClick(int button) {
        if (button == 0) this.setActive(!this.isActive());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.onClick(button);
            return true;
        }
        return false;
    }
}
