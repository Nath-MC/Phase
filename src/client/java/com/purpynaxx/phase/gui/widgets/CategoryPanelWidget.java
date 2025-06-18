package com.purpynaxx.phase.gui.widgets;

import com.purpynaxx.phase.modules.impl.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryPanelWidget implements Drawable, Element {

    private static final int dragThresholdSquared = 9;
    private static final int titleBarHeight = 15;

    private static final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private static final int backgroundColor = new Color(20, 20, 20, 200).getRGB();
    private static final int titleBarColor = new Color(40, 40, 40, 255).getRGB();
    private static final int borderColor = new Color(80, 80, 80, 255).getRGB();
    private static final int titleColor = Color.WHITE.getRGB();

    private static final int width = 100;

    private final String title;

    private final Module.Category category;

    private final List<ModuleWidget> children = new ArrayList<>();

    private final int screenWidth;
    private final int screenHeight;
    private int height;

    private int x;
    private int y;

    private boolean dragging;
    private boolean potentialDrag;
    private double dragOffsetX;
    private double dragOffsetY;

    private boolean collapsed;
    private boolean hovered;
    private double lastClickX;
    private double lastClickY;


    public CategoryPanelWidget(Module.Category category, int x, int y, int screenWidth, int screenHeight, boolean collapsed) {
        this.category = category;
        this.title = category.getFriendlyName();
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.collapsed = collapsed;
    }

    public String getCategoryId() {
        return category.getConstant();
    }

    public void addModuleEntry(Module module) {
        final int moduleHeight = 16;
        int moduleX = this.x;
        int moduleY = this.y + titleBarHeight;

        if (!children.isEmpty()) {
            ModuleWidget lastModule = children.getLast();
            moduleY = lastModule.getY() + lastModule.getFinalHeight();
        }

        ModuleWidget moduleWidget = new ModuleWidget(module, moduleX, moduleY, width, moduleHeight);

        // Add listener for module collapse/expand events
        moduleWidget.setCollapseListener(this::updatePanelLayout);

        this.children.add(moduleWidget);

        updatePanelHeight();
    }

    private void updatePanelLayout() {
        // Recalculate positions of all module widgets
        int currentY = this.y + titleBarHeight;

        for (ModuleWidget moduleWidget : children) {
            moduleWidget.setX(this.x);
            moduleWidget.setY(currentY);
            currentY += moduleWidget.getFinalHeight();
        }

        // Update the panel height
        updatePanelHeight();
    }

    private void updatePanelHeight() {
        if (children.isEmpty()) {
            this.height = titleBarHeight;
            return;
        }

        ModuleWidget lastModule = children.getLast();
        this.height = (lastModule.getY() - this.y) + lastModule.getFinalHeight();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public @Nullable ModuleWidget getHoveredModuleWidget() {
        for (ModuleWidget moduleWidget : children) {
            if (moduleWidget.isHovered()) {
                return moduleWidget;
            }
        }
        return null;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, boolean renderTooltips) {
        // Draw title bar
        context.fill(x, y, x + width, y + titleBarHeight, titleBarColor);
        context.drawText(textRenderer, title, x + 4, y + (titleBarHeight - textRenderer.fontHeight) / 2 + 1, titleColor, false);

        if (this.isCollapsed()) {
            context.drawBorder(x - 1, y - 1, width + 2, titleBarHeight + 2, borderColor);
            return;
        }

        // Draw panel background
        context.fill(x, y + titleBarHeight, x + width, y + height, backgroundColor);
        context.drawBorder(x - 1, y - 1, width + 2, height + 2, borderColor);

        // Reset hover state for all modules
        for (ModuleWidget moduleWidget : children) {
            moduleWidget.setHovered(false);
        }

        // Set hover state for the module under the mouse
        if (this.isHovered()) {
            for (ModuleWidget moduleWidget : children) {
                if (moduleWidget.isMouseOver(mouseX, mouseY, true)) {
                    moduleWidget.setHovered(true);
                    break;
                }
            }
        }

        // Render all module widgets
        for (ModuleWidget moduleWidget : children) {
            moduleWidget.render(context, mouseX, mouseY, delta, renderTooltips);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        render(context, mouseX, mouseY, delta, true);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    private void setLastClick(double x, double y) {
        this.lastClickX = x;
        this.lastClickY = y;
    }

    private void setDragOffset(double mouseX, double mouseY) {
        this.dragOffsetX = mouseX - x;
        this.dragOffsetY = mouseY - y;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (mouseY >= y && mouseY <= y + titleBarHeight) {
                if (button == 0) {
                    potentialDrag = true;
                    this.setLastClick(mouseX, mouseY);
                    return true;
                }
            }

            if (!this.isCollapsed()) {
                for (ModuleWidget moduleWidget : children) {
                    if (moduleWidget.isMouseOver(mouseX, mouseY, false)) {
                        if (moduleWidget.mouseClicked(mouseX, mouseY, button)) {
                            return true;
                        }
                    }
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            if (potentialDrag) {
                double dx = mouseX - this.lastClickX;
                double dy = mouseY - this.lastClickY;
                if (dx * dx + dy * dy > dragThresholdSquared) {
                    dragging = true;
                    potentialDrag = false;
                    this.setDragOffset(mouseX, mouseY);
                    this.updatePosition(mouseX, mouseY);
                    return true;
                }
                return true;
            }

            if (dragging) {
                this.updatePosition(mouseX, mouseY);
                return true;
            }
        }

        for (Element child : children) {
            if (child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        return false;
    }

    private void updatePosition(double mouseX, double mouseY) {
        int x = (int) (mouseX - this.dragOffsetX);
        int y = (int) (mouseY - this.dragOffsetY);

        int clampedX = Math.clamp(x, 0, screenWidth - width);
        int clampedY = Math.clamp(y, 0, screenHeight - titleBarHeight);

        this.setX(clampedX);
        this.setY(clampedY);

        // Update the positions of all modules
        updatePanelLayout();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (button == 0) {
            if (potentialDrag) {
                this.setCollapsed(!this.isCollapsed());
                potentialDrag = false;
                handled = true;
            }
            if (dragging) {
                dragging = false;
                handled = true;
            }
        }

        for (Element child : children) {
            handled |= child.mouseReleased(mouseX, mouseY, button);
        }

        return handled;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int panelHeight = this.isCollapsed() ? titleBarHeight : this.height;
        return mouseX >= this.x && mouseX <= this.x + width && mouseY >= this.y && mouseY <= this.y + panelHeight;
    }

    public boolean isDragging() {
        return dragging || potentialDrag;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {}

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }
}