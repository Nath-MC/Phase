package com.purpynaxx.phase.gui.widgets;

import com.purpynaxx.phase.gui.widgets.settings.BooleanWidget;
import com.purpynaxx.phase.gui.widgets.settings.CyclingWidget;
import com.purpynaxx.phase.gui.widgets.settings.SettingWidget;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.settings.BooleanSetting;
import com.purpynaxx.phase.settings.CyclingSetting;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleWidget implements Element, Drawable {

    private static final Map<Class<?>, SettingWidgetFactory<?>> SETTING_WIDGET_FACTORIES = new HashMap<>();
    private static final TextRenderer TEXT_RENDERER = MinecraftClient.getInstance().textRenderer;

    static {
        SETTING_WIDGET_FACTORIES.put(BooleanSetting.class, (SettingWidgetFactory<BooleanSetting>) BooleanWidget::new);
        SETTING_WIDGET_FACTORIES.put(CyclingSetting.class, (setting, x, y, width) -> new CyclingWidget((CyclingSetting<?>) setting, x, y, width));
    }

    private final Module module;

    private final List<Setting<?>> moduleSettings;
    private final List<SettingWidget<?>> settingWidgets = new ArrayList<>();

    private final @Nullable TooltipWidget tooltipWidget;

    private final Text displayName;

    private final int width;
    private final int height;

    private final AnimationState hoverAnimation = new AnimationState();
    private final AnimationState tooltipAnimation = new AnimationState();

    private CollapseListener collapseListener;
    private int settingsHeight;

    private boolean hovered;
    private boolean collapsed = true;

    private int x;
    private int y;

    public ModuleWidget(Module module, int x, int y, int width, int height) {
        this.module = module;
        this.moduleSettings = module.getSettings();
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.displayName = Text.of(module.getName());
        this.tooltipWidget = createTooltipWidget(module);
        initSettingWidgets();
    }

    private @Nullable TooltipWidget createTooltipWidget(Module module) {
        Text description = module.getDescription();
        if (description != null && !description.getString().isEmpty()) {
            return new TooltipWidget(description);
        }
        return null;
    }

    private void initSettingWidgets() {
        int settingY = this.y + this.height;

        for (Setting<?> setting : moduleSettings) {
            SettingWidget<?> widget = createSettingWidget(setting, this.x, settingY, this.width);
            if (widget != null) {
                settingWidgets.add(widget);
                int widgetHeight = widget.getHeight();
                settingY += widgetHeight;
                settingsHeight += widgetHeight;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private SettingWidget<?> createSettingWidget(Setting<?> setting, int x, int y, int width) {
        SettingWidgetFactory<?> factory = SETTING_WIDGET_FACTORIES.get(setting.getClass());
        if (factory != null) {
            return ((SettingWidgetFactory<Setting<?>>) factory).create(setting, x, y, width);
        }
        return null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(context, mouseX, mouseY, delta, true);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, boolean renderTooltips) {
        updateAnimations(delta);
        renderModuleHeader(context);

        if (!collapsed) {
            renderSettingsPanel(context, mouseX, mouseY, delta);
        }

        if (renderTooltips) {
            renderTooltip(context, mouseX, mouseY, delta);
        }
    }

    private void updateAnimations(float delta) {
        hoverAnimation.update(hovered, delta, UIConstants.ANIMATION_SPEED,
                hovered ? UIConstants.HOVER_ANIMATION_MULTIPLIER : UIConstants.HOVER_DECAY_MULTIPLIER);

        tooltipAnimation.update(hovered, delta, UIConstants.ANIMATION_SPEED, 1.0f);
        if (!hovered) {
            tooltipAnimation.update(false, 1.0f, 1.0f, 1.0f); // Reset tooltip timer immediately
        }
    }

    private void renderModuleHeader(DrawContext context) {
        float hoverProgress = hoverAnimation.getValue();

        // Render background based on module state
        if (module.isActive()) {
            int backgroundColor = interpolateColor(
                    UIConstants.ACTIVE_COLOR_BASE,
                    UIConstants.ACTIVE_COLOR_HOVER,
                    hoverProgress);

            context.fill(x, y, x + width, y + height, backgroundColor);
        } else if (hoverProgress > 0) {
            int alpha = (int) (hoverProgress * 60);
            context.fill(x, y, x + width, y + height, new Color(255, 255, 255, alpha).getRGB());
        }

        // Render module name
        int textColor = module.isActive()
                ? UIConstants.TEXT_COLOR_ACTIVE
                : interpolateColor(UIConstants.TEXT_COLOR_INACTIVE, UIConstants.TEXT_COLOR_ACTIVE, hoverProgress);

        int textX = x + width / 2 - TEXT_RENDERER.getWidth(displayName) / 2;
        int textY = y + (height - TEXT_RENDERER.fontHeight) / 2 + 1;
        context.drawText(TEXT_RENDERER, displayName, textX, textY, textColor, false);
    }

    private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render divider
        context.fill(x, y + height, x + width, y + height + 1, UIConstants.DIVIDER_COLOR);

        // Render settings
        for (SettingWidget<?> widget : settingWidgets) {
            widget.render(context, mouseX, mouseY, delta);
        }
    }

    public void renderTooltip(DrawContext context, int mouseX, int mouseY, float delta) {
        if (tooltipWidget != null && tooltipAnimation.getValue() >= 1.0f) {
            tooltipWidget.refreshPos(x, y, 108, 8, context.getScaledWindowWidth(), context.getScaledWindowHeight());
            tooltipWidget.render(context, mouseX, mouseY, delta);
        }
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, true)) {
            if (button == 0) {
                module.toggle(); // Use module's toggle method directly
                return true;
            } else if (button == 1) {
                toggleCollapsed();
                return true;
            }
        } else if (!collapsed) {
            for (SettingWidget<?> widget : settingWidgets) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void toggleCollapsed() {
        setCollapsed(!collapsed);
    }

    public void updatePosition(int x, int y) {
        this.x = x;
        this.y = y;

        int settingY = this.y + this.height;
        for (SettingWidget<?> widget : settingWidgets) {
            widget.setX(x);
            widget.setY(settingY);
            settingY += widget.getHeight();
        }
    }

    public void setCollapsed(boolean collapsed) {
        boolean wasCollapsed = this.collapsed;
        this.collapsed = collapsed;

        if (wasCollapsed != collapsed && collapseListener != null) {
            collapseListener.onCollapseStateChanged();
        }
    }

    public void setCollapseListener(CollapseListener listener) {
        this.collapseListener = listener;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public boolean isMouseOver(double mouseX, double mouseY, boolean headerOnly) {
        if (headerOnly) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + getFinalHeight();
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    public int getY() {
        return y;
    }

    public void setY(int y) {
        updatePosition(this.x, y);
    }

    public void setX(int x) {
        updatePosition(x, this.y);
    }

    public boolean hasTooltip() {
        return tooltipWidget != null;
    }

    public boolean isTooltipReady() {
        return tooltipAnimation.getValue() >= 1.0f;
    }

    public int getFinalHeight() {
        return collapsed ? height : height + settingsHeight;
    }

    public interface CollapseListener {

        void onCollapseStateChanged();

    }

    @FunctionalInterface
    private interface SettingWidgetFactory<T extends Setting<?>> {

        SettingWidget<?> create(T setting, int x, int y, int width);

    }

    private static class UIConstants {

        static final int ACTIVE_COLOR_BASE = new Color(255, 255, 255, 40).getRGB();
        static final int ACTIVE_COLOR_HOVER = new Color(255, 255, 255, 70).getRGB();
        static final int DIVIDER_COLOR = new Color(80, 80, 80, 255).getRGB();
        static final int TEXT_COLOR_ACTIVE = Color.WHITE.getRGB();
        static final int TEXT_COLOR_INACTIVE = Color.LIGHT_GRAY.getRGB();
        static final float ANIMATION_SPEED = 0.1f;
        static final float HOVER_ANIMATION_MULTIPLIER = 5.0f;
        static final float HOVER_DECAY_MULTIPLIER = 3.0f;

    }

    private static class AnimationState {

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