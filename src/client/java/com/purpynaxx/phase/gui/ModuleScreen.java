package com.purpynaxx.phase.gui;

import com.purpynaxx.phase.config.IOManager;
import com.purpynaxx.phase.gui.serialization.Container;
import com.purpynaxx.phase.gui.serialization.PanelState;
import com.purpynaxx.phase.gui.widgets.ContainerPanelWidget;
import com.purpynaxx.phase.gui.widgets.ModuleWidget;
import com.purpynaxx.phase.modules.Categories;
import com.purpynaxx.phase.modules.Category;
import com.purpynaxx.phase.modules.Module;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.modules.visuals.GUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.purpynaxx.phase.Phase.LOGGER;

public class ModuleScreen extends Screen {

    private static final Path STATES_PATH = Path.of("gui/states");
    private static final int PANEL_PADDING = 16;
    private static final Modules modules = Modules.getInstance();
    private static final Categories categories = Categories.getInstance();
    private static Container currentContainer = new Container(new ArrayList<>(), 0, 0);
    private final List<ContainerPanelWidget> panels = new ArrayList<>();

    private final @Nullable Screen parent;

    public ModuleScreen(String title, @Nullable Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panels.clear();
        loadPanelStates();

        List<PanelState> savedStates = currentContainer.panelStates();
        int categoryCount = categories.getCategoryCount();

        if (shouldCreateDefaultLayout(savedStates, categoryCount)) {
            createDefaultPanelLayout(categoryCount);
        } else {
            restoreSavedPanelLayout(savedStates);
        }
    }

    private boolean shouldCreateDefaultLayout(List<PanelState> savedStates, int categoryCount) {
        return savedStates.isEmpty() || categoryCount != savedStates.size();
    }

    private void loadPanelStates() {
        currentContainer = IOManager.loadData(STATES_PATH,
                Container.CODEC,
                () -> new Container(new ArrayList<>(), this.width, this.height));
    }

    private void createDefaultPanelLayout(int categoryCount) {
        int totalWidth = (categoryCount * ContainerPanelWidget.WIDTH) + Math.max(0, categoryCount - 1) * PANEL_PADDING; // calculate total panels width
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 10;

        final int[] index = {0};
        categories.getCategories().stream().sorted(Comparator.comparing(Category::getFriendlyName, String::compareToIgnoreCase)).forEach(category -> {
            int x = startX + (index[0] * (ContainerPanelWidget.WIDTH + PANEL_PADDING));

            try {
                createAndPopulatePanel(category, x, startY, false);
            } catch (Exception e) {
                LOGGER.error("Error creating default panel for category '{}'", category.getFriendlyName(), e);
            }
            index[0]++;
        });
    }

    private void restoreSavedPanelLayout(List<PanelState> savedStates) {
        int savedWidth = currentContainer.screenWidth();
        int savedHeight = currentContainer.screenHeight();

        for (PanelState state : savedStates) {
            try {
                Category category = Category.valueOf(state.title().toUpperCase());
                int x = calculateScaledCoordinate(state.x(), savedWidth, this.width);
                int y = calculateScaledCoordinate(state.y(), savedHeight, this.height);

                x = Math.clamp(x, 0, this.width - ContainerPanelWidget.WIDTH);
                y = Math.clamp(y, 0, this.height - 15);

                createAndPopulatePanel(category, x, y, state.collapsed());
            } catch (IllegalArgumentException e) {
                LOGGER.error("No category found for title '{}' from config. Skipping panel.", state.title(), e);
            } catch (Exception e) {
                LOGGER.error("Error creating panel for '{}' from config.", state.title(), e);
            }
        }
    }

    private int calculateScaledCoordinate(int coordinate, int oldDimension, int newDimension) {
        return oldDimension == newDimension ?
                coordinate :
                (int) ((float) coordinate / (float) oldDimension * newDimension);
    }

    private void createAndPopulatePanel(Category category, int x, int y, boolean collapsed) {
        Set<Module> modules = categories.getModulesIn(category);
        ContainerPanelWidget panelWidget = new ContainerPanelWidget(category, x, y, this.width, this.height, collapsed);

        if (!modules.isEmpty()) {
            modules.stream()   // Sort modules by name in natural order
                    .sorted(Comparator.comparing(Module::getName, String::compareToIgnoreCase))
                    .forEach(panelWidget::addModuleEntry);
        } else {
            LOGGER.warn("No modules registered for the category {}", category.name());
        }

        this.panels.add(panelWidget);
    }

    private void saveStates() {
        List<PanelState> statesToSave = new ArrayList<>();
        for (ContainerPanelWidget panel : this.panels) {
            statesToSave.add(new PanelState(
                    panel.getCategoryId(),
                    panel.getX(),
                    panel.getY(),
                    panel.isCollapsed()
            ));
        }

        Container container = new Container(statesToSave, this.width, this.height);
        IOManager.saveData(STATES_PATH, Container.CODEC, container);
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
    public void close() {
        this.saveStates();
        this.client.setScreen(this.parent);

        Module GUIModule = modules.getModule(GUI.class);
        boolean currentState = GUIModule.isActive();

        if (currentState) {
            GUIModule.toggle();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, delta);
        renderPanels(context, mouseX, mouseY, delta);
    }

    private void renderBackground(DrawContext context, float delta) {
        if (this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
    }

    private void renderPanels(DrawContext context, int mouseX, int mouseY, float delta) {
        ModuleWidget hoveredModule = findHoveredModuleWidget(mouseX, mouseY);

        this.panels.forEach(panel -> panel.render(context, mouseX, mouseY, delta, false));

        if (hoveredModule != null && hoveredModule.hasTooltip() && hoveredModule.isTooltipReady()) {
            hoveredModule.renderTooltip(context, mouseX, mouseY, delta);
        }
    }

    private @Nullable ModuleWidget findHoveredModuleWidget(int mouseX, int mouseY) {
        ModuleWidget hoveredModule = null;
        boolean foundHoveredPanel = false;

        for (ContainerPanelWidget panel : this.panels.reversed()) {
            if (panel.isMouseOver(mouseX, mouseY) && !foundHoveredPanel) {
                panel.setHovered(true);
                foundHoveredPanel = true;

                ModuleWidget widget = panel.getHoveredModuleWidget();
                if (widget != null && widget.hasTooltip() && widget.isTooltipReady()) {
                    hoveredModule = widget;
                }
            } else {
                panel.setHovered(false);
            }
        }

        return hoveredModule;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ContainerPanelWidget panel : this.panels.reversed()) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                bringPanelToFront(panel);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void bringPanelToFront(ContainerPanelWidget panel) {
        int currentIndex = this.panels.indexOf(panel);
        if (currentIndex != this.panels.size() - 1) {
            this.panels.remove(panel);
            this.panels.add(panel);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (ContainerPanelWidget panel : this.panels.reversed()) {
            if (panel.isDragging()) {
                return panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean released = false;
        for (ContainerPanelWidget panel : this.panels.reversed()) {
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

}