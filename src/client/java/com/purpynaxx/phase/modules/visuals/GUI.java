package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.modules.impl.ModuleBase;
import com.purpynaxx.phase.ui.ModuleScreen;

public class GUI extends ModuleBase {

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
