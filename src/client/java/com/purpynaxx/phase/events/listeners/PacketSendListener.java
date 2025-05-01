package com.purpynaxx.phase.events.listeners;

import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface PacketSendListener {
    void onPacketSend(Packet<?> packet, CallbackInfo event);
}
