package com.purpynaxx.phase.events.network;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public abstract class PacketCallback {

    public static final Event<IN> IN = EventFactory.createArrayBacked(IN.class, callbacks -> (packet, event) -> {
        for (IN event1 : callbacks)
            event1.onPacketReceive(packet, event);
    });

    public static final Event<OUT> OUT = EventFactory.createArrayBacked(OUT.class, callbacks -> (packet, event) -> {
        for (OUT event1 : callbacks)
            event1.onPacketSend(packet, event);
    });

    @FunctionalInterface
    public interface IN {
        void onPacketReceive(Packet<?> packet, CallbackInfo event);
    }

    @FunctionalInterface
    public interface OUT {
        void onPacketSend(Packet<?> packet, CallbackInfo event);
    }
}
