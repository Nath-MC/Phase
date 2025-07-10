package com.purpynaxx.phase.events.interfaces.network;

import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class PacketHandler {

    private PacketHandler() {}

    public interface IN {

        void onPacketReceive(Packet<?> packet, CallbackInfo event);

    }

    public interface OUT {

        void onPacketSend(Packet<?> packet, CallbackInfo event);

    }

}
