package com.purpynaxx.phase.modules.visuals;

import com.purpynaxx.phase.events.interfaces.world.WorldRender;
import com.purpynaxx.phase.helpers.render.DrawMode;
import com.purpynaxx.phase.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.awt.*;

public class ESP extends Module implements WorldRender.END {

    private static final Text description = Text.translatable("modules.visuals.esp.description");
    private static final Color color = new Color(255, 0, 0, 150);

    private ESP() {
        super(description);
    }

    @Override
    public void onWorldRenderEnd(WorldRenderContext context) {
        Iterable<Entity> entities = context.world().getEntities();

        for (Entity entity : entities) {
            if (!entity.isPlayer()) continue;

            PlayerEntity player = (PlayerEntity) entity;

            if (player.isMainPlayer()) continue;

            if (client.player.isSpectator() && client.getCameraEntity() == player) continue;

            renderer.drawBoxForEntity(context, entity, color, DrawMode.BOTH, false);
        }
    }


}
