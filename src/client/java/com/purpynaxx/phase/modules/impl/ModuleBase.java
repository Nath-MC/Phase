package com.purpynaxx.phase.modules.impl;

import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public abstract class ModuleBase {

    protected static ModuleBase instance; // each module should be a singleton
    protected final Category category;
    protected final String name;
    protected final String desc;
    protected final Logger logger;
    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected boolean active;
    protected ModuleBase(String desc) {
        this.category = this.setCategory();
        this.name = this.getClass().getSimpleName();
        this.desc = desc;
        this.active = false;
        this.logger = LoggerFactory.getLogger(this.name);
    }

    public Category getCategory() {
        return category;
    }

    private Category setCategory() {
        String packageName = this.getClass().getPackageName();
        return Category.valueOf(packageName.substring(packageName.lastIndexOf(".") + 1).toUpperCase(Locale.ROOT));
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) this.onActivation();
        else this.onDeactivation();
    }

    public void onActivation() {}

    public void onDeactivation() {}
}
