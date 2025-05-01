package com.purpynaxx.phase.events.listeners;

import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface PacketReceiveListener {
    void onPacketReceive(Packet<?> packet, CallbackInfo event);
}
