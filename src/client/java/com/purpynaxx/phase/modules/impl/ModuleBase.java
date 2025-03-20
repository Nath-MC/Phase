package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.Phase;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

public abstract class ModuleBase {

    protected static ModuleBase instance; // each module should be a singleton
    protected final String name;
    protected final String desc;
    protected final Logger logger = Phase.logger;
    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected boolean active;

    protected ModuleBase(String desc) {
        this.name = this.getClass().getSimpleName();
        this.desc = desc;
        this.active = false;
    }

    protected abstract void run();


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
