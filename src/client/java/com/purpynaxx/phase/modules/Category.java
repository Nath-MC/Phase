package com.purpynaxx.phase.modules;

import net.minecraft.text.Text;

import java.util.Locale;

public enum Category {
    COMBAT(Text.translatable("modules.category.combat.title")),
    MISCELLANEOUS(Text.translatable("modules.category.miscellaneous.title")),
    MOVEMENTS(Text.translatable("modules.category.movements.title")),
    VISUALS(Text.translatable("modules.category.visuals.title")),
    ;

    private final Text translation;

    Category(Text translation) {
        this.translation = translation;
    }

    public String getConstant() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public String getFriendlyName() {
        return this.translation.getString();
    }
}
