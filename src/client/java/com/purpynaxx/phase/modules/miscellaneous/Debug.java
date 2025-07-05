package com.purpynaxx.phase.modules.miscellaneous;

import com.purpynaxx.phase.modules.Module;
import net.minecraft.text.Text;

public class Debug extends Module {

    private static final Text description = Text.translatable("modules.miscellaneous.debug.description");

    private Debug() {
        super(description);
    }

}
