package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.settings.BooleanSetting;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Module {

    protected static final MinecraftClient client = MinecraftClient.getInstance();

    protected final String name;
    protected final String description;
    protected final Category category;
    protected final Logger logger;

    private final List<Setting<?>> settings = new ArrayList<>();
    private final Setting<Boolean> active;


    protected Module(String description) {
        this.name = this.getClass().getSimpleName();
        this.description = description;
        this.category = determineCategory();
        this.logger = LoggerFactory.getLogger(this.name);
        this.active = registerSetting(new BooleanSetting("Active", "Current module state", false));
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    private Category determineCategory() {
        String packageName = this.getClass().getPackageName();
        return Category.valueOf(packageName.substring(packageName.lastIndexOf(".") + 1).toUpperCase(Locale.ROOT));
    }

    protected <T extends Setting<?>> T registerSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public final boolean isActive() {
        return active.getValue();
    }

    public final void setActive(boolean active) {
        if (active == this.active.getValue()) return;
        this.toggle();
    }

    public final void toggle() {
        boolean newState = !this.active.getValue();
        this.active.setValue(newState);

        if (newState) {
            try {
                onActivate();
            } catch (Exception e) {
                logger.error("Error during module activation", e);
            }
        } else {
            try {
                onDeactivate();
            } catch (Exception e) {
                logger.error("Error during module deactivation", e);
            }
        }
    }

    protected void onActivate() {}

    protected void onDeactivate() {}

    public enum Category {
        VISUALS,
        MOVEMENTS;

        public String getFriendlyName() {
            String name = this.name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        }
    }
}