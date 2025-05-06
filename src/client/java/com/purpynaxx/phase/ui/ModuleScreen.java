package com.purpynaxx.phase.ui;

import com.purpynaxx.phase.Phase;
import com.purpynaxx.phase.modules.impl.Category;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import com.purpynaxx.phase.modules.visuals.GUI;
import com.purpynaxx.phase.ui.widgets.CategoryPanelWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;


public class ModuleScreen extends Screen {

    private final List<CategoryPanelWidget> panels = new ArrayList<>();
    private final ModuleManager manager = ModuleManager.getInstance();
    private final Screen parent;

    public ModuleScreen(String title, Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        //TODO Save state and restore it, being able to scale current positions after resizing
        panels.clear();
        for (Category category : manager.getCategories()) {
            CategoryPanelWidget panelWidget = new CategoryPanelWidget(this.textRenderer, category.getFriendlyName(), this.width, this.height);
            manager.getModulesByCategoryMap().get(category).forEach(panelWidget::addModuleEntry);
            panels.add(panelWidget);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        boolean skip = false;
        for (CategoryPanelWidget panel : panels.reversed()) {
            if (panel.isMouseOver(mouseX, mouseY) && !skip) {
                panel.setHovered(true);
                skip = true;
            } else panel.setHovered(false);
        }

        panels.forEach(panel -> panel.render(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CategoryPanelWidget panel : panels.reversed()) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                if (panels.indexOf(panel) != panels.size() - 1) {
                    panels.remove(panel);
                    panels.add(panel);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (CategoryPanelWidget panel : panels.reversed()) {
            if (panel.isDragging()) {
                return panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean released = false;
        for (CategoryPanelWidget panel : panels.reversed()) {
            if (panel.mouseReleased(mouseX, mouseY, button)) {
                released = true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button) || released;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Phase.keyBinding.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        // TODO save windows state
        this.client.setScreen(parent);
        manager.setModuleActive(GUI.class, false);
    }
}
