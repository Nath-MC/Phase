package com.purpynaxx.phase.modules.combat;

import com.purpynaxx.phase.modules.Module;
import net.minecraft.text.Text;

public class Aura extends Module {

    private static final Text description = Text.translatable("modules.combat.aura.description");

    private Aura() {
        super(description);
    }

}
