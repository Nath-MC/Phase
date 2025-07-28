package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.gui.ModuleScreen;
import com.purpynaxx.phase.modules.Module;
import net.minecraft.text.Text;

public class GUI extends Module {

    private final static Text title = Text.translatable("interface.gui.title");
    private final static Text description = Text.translatable("interface.gui.description");

    private GUI() {
        super(description);
    }

    @Override
    public void onActivate() {
        client.setScreen(new ModuleScreen(title.getString(), client.currentScreen));
    }

    @Override
    public void onDeactivate() {
        if (client.currentScreen instanceof ModuleScreen)
            client.currentScreen.close();
    }

}
