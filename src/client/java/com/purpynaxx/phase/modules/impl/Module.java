package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.settings.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public abstract class Module {

    protected final Category category;
    protected final String name;
    protected final String desc;
    protected final Logger logger;
    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected final BooleanSetting active;

    protected Module(String desc) {
        this.category = this.setCategory();
        this.name = this.getClass().getSimpleName();
        this.desc = desc;
        this.active = new BooleanSetting("Active", "Current module state", false);
        this.logger = LoggerFactory.getLogger(this.name);
    }

    public final Category getCategory() {
        return category;
    }

    private Category setCategory() {
        String packageName = this.getClass().getPackageName();
        return Category.valueOf(packageName.substring(packageName.lastIndexOf(".") + 1).toUpperCase(Locale.ROOT));
    }

    public final String getName() {
        return name;
    }

    public final String getDesc() {
        return desc;
    }

    public final boolean isActive() {
        return active.getValue();
    }

    public final void setActive(boolean active) {
        if (active == this.active.getValue()) return;
        this.toggle();
    }

    public final void toggle() {
        this.active.toggle();
        boolean currentState = this.active.getValue();
        if (currentState) this.onActivation();
        else this.onDeactivation();
    }

    public void onActivation() {}

    public void onDeactivation() {}

    public enum Category {
        VISUALS,
        MOVEMENTS;

        public String getFriendlyName() {
            String name = this.name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        }
    }
}
