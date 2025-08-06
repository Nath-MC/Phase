package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.modules.Module;
import net.minecraft.text.Text;

public class NoRotation extends Module {

    private static final Text description = Text.of("Cancels all server-side rotation");

    private NoRotation() {
        super(description);
    }

}
