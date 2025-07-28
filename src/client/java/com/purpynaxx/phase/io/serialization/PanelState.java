package com.purpynaxx.phase.io.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PanelState(String title, int x, int y, boolean collapsed) {

    public static final Codec<PanelState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("title").forGetter(PanelState::title),
                    Codec.INT.fieldOf("x").forGetter(PanelState::x),
                    Codec.INT.fieldOf("y").forGetter(PanelState::y),
                    Codec.BOOL.fieldOf("collapsed").forGetter(PanelState::collapsed)
            ).apply(instance, PanelState::new)
    );

}
