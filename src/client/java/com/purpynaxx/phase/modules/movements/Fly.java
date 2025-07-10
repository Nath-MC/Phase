package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.modules.Module;
import net.minecraft.text.Text;

public class Fly extends Module {

    private static final Text description = Text.translatable("modules.movements.fly.description");

    private Fly() {
        super(description);
    }

}
