package com.purpynaxx.phase.gui;

import com.mojang.serialization.Codec;
import com.purpynaxx.phase.Phase;
import com.purpynaxx.phase.config.ConfigManager;
import com.purpynaxx.phase.gui.serialization.Container;
import com.purpynaxx.phase.gui.serialization.PanelState;
import com.purpynaxx.phase.gui.widgets.CategoryPanelWidget;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import com.purpynaxx.phase.modules.visuals.GUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.purpynaxx.phase.Phase.logger;

public class ModuleScreen extends Screen {

    private static final File savedStatesFile = ConfigManager.getConfigFile("gui/states");
    private static final Codec<Container> containerCodec = Container.CODEC;

    private static Container currentContainer = new Container(new ArrayList<>(), 0, 0);
    private final List<CategoryPanelWidget> panels = new ArrayList<>();
    private final ModuleManager manager = ModuleManager.getInstance();
    private final @Nullable Screen parent;


    public ModuleScreen(String title, @Nullable Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    private void saveStates() {
        List<PanelState> statesToSave = new ArrayList<>();
        for (CategoryPanelWidget panel : this.panels)
            statesToSave.add(new PanelState(panel.getTitle(), panel.getX(), panel.getY(), panel.isCollapsed()));
        Container container = new Container(statesToSave, this.width, this.height);
        ConfigManager.saveData(savedStatesFile, containerCodec, container);
        currentContainer = container;
    }


    @Override
    public void resize(MinecraftClient client, int width, int height) {
        this.saveStates();
        this.width = width;
        this.height = height;
        this.init();
    }

    @Override
    protected void init() {
        this.panels.clear();
        currentContainer = ConfigManager.loadData(savedStatesFile, containerCodec, () -> new Container(new ArrayList<>(), this.width, this.height));
        List<PanelState> savedStates = currentContainer.panelStates();
        int categories = this.manager.getCategories().size();
        if (savedStates.isEmpty() || categories != savedStates.size()) {
            logger.info("Using default configuration");
            int index = 0;
            final int padding = 16;
            final int panelWidth = 100;
            int totalPanelsAndPaddingWidth = (categories * panelWidth) + Math.max(0, categories - 1) * padding;
            int startX = (this.width - totalPanelsAndPaddingWidth) / 2;

            for (Module.Category category : this.manager.getCategories()) {
                int x = startX + (index * panelWidth) + (index * padding);
                int y = this.height / 10;
                this.createAndPopulatePanel(category, x, y, false);
                index++;
            }
        } else {
            int savedWidth = currentContainer.screenWidth();
            int savedHeight = currentContainer.screenHeight();

            for (PanelState state : savedStates) {
                try {
                    Module.Category category = Module.Category.valueOf(state.title().toUpperCase());
                    int x = savedWidth == this.width ? state.x() : (int) ((float) state.x() / (float) savedWidth * this.width);
                    int y = savedHeight == this.height ? state.y() : (int) ((float) state.y() / (float) savedHeight * this.height);
                    x = Math.clamp(x, 0, this.width - 100);
                    y = Math.clamp(y, 0, this.height - 15);

                    this.createAndPopulatePanel(category, x, y, state.collapsed());
                } catch (IllegalArgumentException e) {
                    logger.error("No category found for title '{}' from config. Skipping panel.", state.title(), e);
                } catch (Exception e) {
                    logger.error("Error creating panel for '{}' from config.", state.title(), e);
                }
            }
        }
    }

    private void createAndPopulatePanel(Module.Category category, int x, int y, boolean collapsed) {
        Set<Module> modules = this.manager.getModulesByCategoryMap().get(category);
        CategoryPanelWidget panelWidget = new CategoryPanelWidget(category.getFriendlyName(), x, y, this.width, this.height, collapsed);
        if (!modules.isEmpty()) modules.forEach(panelWidget::addModuleEntry);
        else logger.warn("No modules was registered for the category {}", category.name());
        this.panels.add(panelWidget);
    }

    @Override
    public void close() {
        this.saveStates();
        this.client.setScreen(this.parent);
        this.manager.setModuleActive(GUI.class, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client.world == null)
            this.renderPanoramaBackground(context, delta);
        context.fill(0, 0, this.width, this.height, 0x67000000);
        boolean skip = false;
        for (CategoryPanelWidget panel : this.panels.reversed()) {
            if (panel.isMouseOver(mouseX, mouseY) && !skip) {
                panel.setHovered(true);
                skip = true;
            } else panel.setHovered(false);
        }
        this.panels.forEach(panel -> panel.render(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CategoryPanelWidget panel : this.panels.reversed()) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                if (this.panels.indexOf(panel) != this.panels.size() - 1) {
                    this.panels.remove(panel);
                    this.panels.add(panel);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (CategoryPanelWidget panel : this.panels.reversed())
            if (panel.isDragging())
                return panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean released = false;
        for (CategoryPanelWidget panel : this.panels.reversed())
            if (panel.mouseReleased(mouseX, mouseY, button)) released = true;
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
}