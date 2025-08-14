package com.purpynaxx.phase.modules.movements;

import com.purpynaxx.phase.events.interfaces.network.PacketHandler;
import com.purpynaxx.phase.modules.Module;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NoRotation extends Module implements PacketHandler.OUT {

    private static final Text description = Text.of("Cancels all server-side rotation");

    private NoRotation() {
        super(description);
    }

    @Override
    public void onPacketSend(Packet<?> packet, CallbackInfo event) {
        if (packet instanceof PlayerMoveC2SPacket movePacket && movePacket.changesLook()) {
            event.cancel();
            PlayerMoveC2SPacket newPacket;

            if (movePacket.changesPosition())
                newPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                        movePacket.getX(0),
                        movePacket.getY(0),
                        movePacket.getZ(0),
                        movePacket.isOnGround(),
                        movePacket.horizontalCollision()
                );
            else
                newPacket = new PlayerMoveC2SPacket.OnGroundOnly(movePacket.isOnGround(), movePacket.horizontalCollision());

            client.player.networkHandler.sendPacket(newPacket);
        }
    }

}
