package com.purpynaxx.phase.modules;

import com.purpynaxx.phase.modules.impl.ModuleBase;

import java.util.Objects;

public class GUI extends ModuleBase {

    private GUI(String desc) {
        super(desc);
    }

    private static ModuleBase getInstance() {
        if (Objects.isNull(instance))
            instance = new GUI("Module Interface");
        return instance;
    }

    @Override
    protected void run() {}

    @Override
    public void onActivation() {}
}
