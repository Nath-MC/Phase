package com.purpynaxx.phase.ui;

import com.purpynaxx.phase.Phase;
import com.purpynaxx.phase.modules.impl.Category;
import com.purpynaxx.phase.modules.impl.ModuleBase;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import com.purpynaxx.phase.modules.visuals.GUI;
import com.purpynaxx.phase.ui.widgets.CategoryPanelWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.*;

import static com.purpynaxx.phase.Phase.logger;


public class ModuleScreen extends Screen {

    private static final Map<String, Object[]> lastPanelsStates = new HashMap<>();
    private final List<CategoryPanelWidget> panels = new ArrayList<>();
    private final ModuleManager manager = ModuleManager.getInstance();
    private final Screen parent;

    public ModuleScreen(String title, Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panels.clear();
        if (lastPanelsStates.isEmpty()) {
            int index = 0;
            int categories = manager.getCategories().size();
            final int padding = 16;
            final int panelWidth = 100;
            int totalPanelsAndPaddingWidth = (categories * panelWidth) + Math.max(0, categories - 1) * padding;
            int startX = (this.width - totalPanelsAndPaddingWidth) / 2;

            for (Category category : manager.getCategories()) {
                int x = startX + (index * panelWidth) + (index * padding);
                int y = this.height / 10;
                this.createAndPopulatePanel(category, x, y, false);
                index++;
            }
        } else {
            for (Map.Entry<String, Object[]> entry : lastPanelsStates.entrySet()) {
                String title = entry.getKey();
                Object[] state = entry.getValue();

                try {
                    Category category = Category.valueOf(title.toUpperCase());
                    int x = (int) state[0];
                    int y = (int) state[1];
                    boolean collapsed = (boolean) state[2];
                    this.createAndPopulatePanel(category, x, y, collapsed);
                } catch (IllegalArgumentException e) {
                    logger.error("No category with the specified name", e);
                }
            }
        }
    }

    private void createAndPopulatePanel(Category category, int x, int y, boolean collapsed) {
        Set<ModuleBase> modules = manager.getModulesByCategoryMap().get(category);
        CategoryPanelWidget panelWidget = new CategoryPanelWidget(category.getFriendlyName(), x, y, this.width, this.height, collapsed);
        modules.forEach(panelWidget::addModuleEntry);
        panels.add(panelWidget);
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
        for (CategoryPanelWidget panel : panels) {
            lastPanelsStates.put(panel.getTitle(), new Object[]{
                    panel.getX(),
                    panel.getY(),
                    panel.isCollapsed()
            });
        }
        this.client.setScreen(parent);
        manager.setModuleActive(GUI.class, false);
    }
}
