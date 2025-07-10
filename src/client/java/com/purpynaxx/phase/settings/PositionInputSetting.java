package com.purpynaxx.phase.settings;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

public class PositionInputSetting extends Setting<BlockPos> {

    public PositionInputSetting(String id, Text name, Text description, @NotNull BlockPos defaultValue) {
        super(id, name, description, defaultValue);
    }

}
