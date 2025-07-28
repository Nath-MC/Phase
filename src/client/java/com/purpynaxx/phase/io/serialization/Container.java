package com.purpynaxx.phase.io.serialization;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record Container(List<PanelState> panelStates, int screenWidth, int screenHeight) {

    public static final Codec<Container> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.list(PanelState.CODEC).fieldOf("panelStates").forGetter(Container::panelStates),
                    Codec.INT.fieldOf("screenWidth").forGetter(Container::screenWidth),
                    Codec.INT.fieldOf("screenHeight").forGetter(Container::screenHeight)
            ).apply(instance, Container::new)
    );

}
