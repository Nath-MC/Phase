package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.gui.ModuleScreen;
import com.purpynaxx.phase.modules.impl.Module;

public class GUI extends Module {

    private final static String title = "Config Screen";

    private GUI() {
        super(title);
    }

    @Override
    public void onActivation() {
        this.client.setScreen(new ModuleScreen(title, this.client.currentScreen));
    }

    @Override
    public void onDeactivation() {
        if (this.client.currentScreen != null && this.client.currentScreen instanceof ModuleScreen)
            this.client.setScreen(null);
    }
}
