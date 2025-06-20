package com.purpynaxx.phase.events.interfaces.network;

import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface PacketHandler {

    interface IN {

        void onPacketReceive(Packet<?> packet, CallbackInfo event);

    }

    interface OUT {

        void onPacketSend(Packet<?> packet, CallbackInfo event);

    }

}
