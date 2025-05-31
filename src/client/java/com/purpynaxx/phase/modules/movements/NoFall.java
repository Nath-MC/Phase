package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.listeners.PacketSendListener;
import com.purpynaxx.phase.mixin.accessors.PlayerMoveC2SPacketAccessor;
import com.purpynaxx.phase.modules.impl.Module;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NoFall extends Module implements PacketSendListener {

    private NoFall() {
        super("Cancel fall damage");
    }

    @Override
    public void onPacketSend(Packet<?> packet, CallbackInfo event) {
        if (packet instanceof PlayerMoveC2SPacket movePacket && client.player.isLoaded() && client.player.getGameMode().isSurvivalLike()) {
            boolean packetOnGround = movePacket.isOnGround();
            if (!packetOnGround && client.player.fallDistance >= 2.9) {
                ((PlayerMoveC2SPacketAccessor) movePacket).setOnGround(true);
            }
        }
    }
}
