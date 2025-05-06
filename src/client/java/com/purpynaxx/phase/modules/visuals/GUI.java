package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.modules.impl.ModuleBase;
import com.purpynaxx.phase.ui.ModuleScreen;
import net.minecraft.text.Text;

import java.util.Objects;

public class GUI extends ModuleBase {

    private final static String title = "Config Screen";

    private GUI(String desc) {
        super(desc);
    }

    @SuppressWarnings("unused")
    private static ModuleBase getInstance() {
        if (Objects.isNull(instance))
            instance = new GUI(title);
        return instance;
    }

    @Override
    public void onActivation() {
        this.client.setScreen(new ModuleScreen(title, this.client.currentScreen));
    }

    @Override
    public void onDeactivation() {
        if (this.client.currentScreen != null && this.client.currentScreen.getTitle().equals(Text.literal(title)))
            this.client.setScreen(null);
    }
}
