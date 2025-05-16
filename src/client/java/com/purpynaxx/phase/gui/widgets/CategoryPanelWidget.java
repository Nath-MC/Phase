package com.purpynaxx.phase.gui.widgets;

import com.purpynaxx.phase.modules.impl.ModuleBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;

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

    private final List<Element> children = new ArrayList<>();
    private final List<Drawable> drawables = new ArrayList<>();

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


    public CategoryPanelWidget(String title, int x, int y, int screenWidth, int screenHeight, boolean collapsed) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.collapsed = collapsed;
    }

    public void addModuleEntry(ModuleBase module) {
        int height = 16;
        int x = this.x;
        int y = this.y + titleBarHeight + children.size() * height;

        ModuleWidget moduleWidget = new ModuleWidget(module, x, y, width, height, textRenderer);

        this.drawables.add(moduleWidget);
        this.children.add(moduleWidget);
        this.height += this.height == 0 ? titleBarHeight + height : height;
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(x, y, x + width, y + titleBarHeight, titleBarColor);
        context.drawText(textRenderer, this.getTitle(), x + 4, y + (titleBarHeight - textRenderer.fontHeight) / 2 + 1, titleColor, false);

        if (this.isCollapsed()) {
            context.drawBorder(x, y, width, titleBarHeight, borderColor);
            return;
        }

        context.fill(x, y + titleBarHeight, x + width, y + height, backgroundColor);
        context.drawBorder(x, y, width, height, borderColor);

        boolean skip = false;
        for (Element element : children) {
            if (this.isHovered() && !skip && element.isMouseOver(mouseX, mouseY)) {
                ((ModuleWidget) element).setHovered(true);
                skip = true;
            } else ((ModuleWidget) element).setHovered(false);
        }

        int contentX = this.x + 1;
        int contentY = y + titleBarHeight;
        int contentEndX = x + width - 1;
        int contentEndY = y + height - 1;

        if (contentEndX < contentX) contentEndX = contentX;
        if (contentEndY < contentY) contentEndY = contentY;

        context.enableScissor(contentX, contentY, contentEndX, contentEndY);
        for (Drawable child : drawables) {
            child.render(context, mouseX, mouseY, delta);
        }
        context.disableScissor();
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

            int contentY = y + titleBarHeight;
            int contentHeight = height - titleBarHeight;

            if (mouseY >= contentY && mouseY <= contentY + contentHeight) {
                for (Element child : children) {
                    if (child.isMouseOver(mouseX, mouseY)) {
                        if (child.mouseClicked(mouseX, mouseY, button)) {
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
            if (child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
                return true;
        }

        return false;
    }

    private void updatePosition(double mouseX, double mouseY) {
        int x = (int) (mouseX - this.dragOffsetX);
        int y = (int) (mouseY - this.dragOffsetY);

        if (x + width <= screenWidth && x >= 0)
            this.setX(x);
        else if (x + width != screenWidth) {
            if (x + width > screenWidth / 2)
                this.setX(screenWidth - width);
            else this.setX(0);
        }

        if (y + titleBarHeight <= screenHeight && y >= 0)
            this.setY(y);
        else if (y + titleBarHeight != screenHeight) {
            if (y + titleBarHeight > screenHeight / 2)
                this.setY(screenHeight - titleBarHeight);
            else this.setY(0);
        }

        int currentY = this.y + titleBarHeight;

        for (Drawable drawable : this.drawables) {
            if (drawable instanceof ModuleWidget moduleWidget) {
                moduleWidget.setX(this.x);
                moduleWidget.setY(currentY);
                currentY += moduleWidget.getHeight();
            }
        }
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
        int height = this.isCollapsed() ? titleBarHeight : this.height;
        return mouseX >= this.x && mouseX <= this.x + width && mouseY >= this.y && mouseY <= this.y + height;
    }

    public boolean isDragging() {
        return dragging || potentialDrag;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean isFocused() {
        // Usually false for a container like this unless it specifically needs focus
        return false;
    }

    // --- Element Methods (Needed for child widget interaction planning, but maybe not fully needed if handled manually) ---
    @Override
    public void setFocused(boolean focused) {
        // Could highlight border if focused
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }
}
