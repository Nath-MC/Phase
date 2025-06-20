package com.purpynaxx.phase.events.interfaces.world;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface WorldRender {

    interface LAST {

        void onWorldRenderLast(WorldRenderContext context);

    }

    interface END {

        void onWorldRenderEnd(WorldRenderContext context);

    }

}
