package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.gui.ModuleScreen;
import com.purpynaxx.phase.modules.impl.Module;
import net.minecraft.text.Text;

import java.util.concurrent.Executors;

public class GUI extends Module {

    private final static Text title = Text.translatable("interface.gui.title");
    private final static Text description = Text.translatable("interface.gui.description");

    private GUI() {
        super(description);
    }

    @Override
    public void onActivate() {
        if (client.isFinishedLoading()) {
            client.setScreen(new ModuleScreen(title.getString(), client.currentScreen));
        } else {
            Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                while (!client.isFinishedLoading()) {
                    Thread.onSpinWait();
                }
                client.execute(() -> client.setScreen(new ModuleScreen(title.getString(), client.currentScreen)));
            });
        }
    }

    @Override
    public void onDeactivate() {
        if (client.currentScreen instanceof ModuleScreen moduleScreen)
            moduleScreen.close();
    }

}
