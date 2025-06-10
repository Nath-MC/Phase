package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.gui.ModuleScreen;
import com.purpynaxx.phase.modules.impl.Module;

public class GUI extends Module {

    private final static String title = "Config Screen";

    private GUI() {
        super(title);
    }

    @Override
    public void onActivate() {
        if (client.isFinishedLoading())
            client.setScreen(new ModuleScreen(title, client.currentScreen));
        else Thread.startVirtualThread(() -> {
            while (!client.isFinishedLoading()) {
                Thread.onSpinWait();
            }
            client.execute(() -> client.setScreen(new ModuleScreen(title, client.currentScreen)));
        });
    }

    @Override
    public void onDeactivate() {
        if (client.currentScreen instanceof ModuleScreen moduleScreen)
            moduleScreen.close();
    }

}
