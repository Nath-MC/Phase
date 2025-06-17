package com.purpynaxx.phase.events;

import com.purpynaxx.phase.events.listeners.*;
import com.purpynaxx.phase.events.network.PacketCallback;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.purpynaxx.phase.Phase.isDevEnvironment;


public class EventManager {

    private static final ModuleManager moduleManager = ModuleManager.getInstance();
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);


    private EventManager() {}

    public static EventManager getInstance() {
        return Holder.instance;
    }

    public void init() {
        Set<Module> modules = moduleManager.getModules();
        int listenersRegistered = 0;
        int modulesScanned = 0;

        for (Module module : modules) {
            modulesScanned++;
            boolean registeredAny = false;

            if (module instanceof WorldStartTickListener listener) {
                ClientTickEvents.START_WORLD_TICK.register(world -> {
                    if (module.isActive() && isReady()) {
                        try {
                            listener.onWorldTickStart(world);
                        } catch (Exception e) {
                            logListenerError(module, "onWorldTickStart", e);
                        }
                    }
                });
                logRegistration(module, "WorldTickStartListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof WorldTickEndListener listener) {
                ClientTickEvents.END_WORLD_TICK.register(world -> {
                    if (module.isActive() && isReady()) {
                        try {
                            listener.onWorldTickEnd(world);
                        } catch (Exception e) {
                            logListenerError(module, "onWorldTickEnd", e);
                        }
                    }
                });
                logRegistration(module, "WorldTickEndListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof ClientTickStartListener listener) {
                ClientTickEvents.START_CLIENT_TICK.register(client -> {
                    if (module.isActive() && isReady()) {
                        try {
                            listener.onClientTickStart(client);
                        } catch (Exception e) {
                            logListenerError(module, "onClientTickStart", e);
                        }
                    }
                });
                logRegistration(module, "ClientTickStartListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof ClientTickEndListener listener) {
                ClientTickEvents.END_CLIENT_TICK.register(client -> {
                    if (module.isActive() && isReady()) {
                        try {
                            listener.onClientTickEnd(client);
                        } catch (Exception e) {
                            logListenerError(module, "onClientTickEnd", e);
                        }
                    }
                });
                logRegistration(module, "ClientTickEndListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof WorldJoinListener listener) {
                ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                    if (module.isActive()) {
                        try {
                            listener.onWorldJoin(handler, sender, client);
                        } catch (Exception e) {
                            logListenerError(module, "onWorldJoin", e);
                        }
                    }
                });
                logRegistration(module, "WorldJoinListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof WorldLeaveListener listener) {
                ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                    if (module.isActive()) {
                        try {
                            listener.onWorldLeave(handler, client);
                        } catch (Exception e) {
                            logListenerError(module, "onWorldLeave", e);
                        }
                    }
                });
                logRegistration(module, "WorldLeaveListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof PacketReceiveListener listener) {
                PacketCallback.IN.register((packet, event) -> {
                    if (module.isActive() && isReady()) {
                        try {
                            listener.onPacketReceive(packet, event);
                        } catch (Exception e) {
                            logListenerError(module, "onPacketReceive", e);
                        }
                    }
                });
                logRegistration(module, "PacketReceiveListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (module instanceof PacketSendListener listener) {
                PacketCallback.OUT.register((packet, event) -> {
                    if (module.isActive() && isReady()) {
                        try {
                            listener.onPacketSend(packet, event);
                        } catch (Exception e) {
                            logListenerError(module, "onPacketSend", e);
                        }
                    }
                });
                logRegistration(module, "PacketSendListener");
                listenersRegistered++;
                registeredAny = true;
            }

            if (!registeredAny && isDevEnvironment) {
                logger.warn("Module {} implements no known listener interfaces.", module.getClass().getSimpleName());
            }
        }
        logger.info("{} listeners were registered across {} scanned modules.", listenersRegistered, modulesScanned);
    }

    private void logRegistration(Module module, String listenerType) {
        if (isDevEnvironment) {
            logger.info("Registered {} as {}", module.getName(), listenerType);
        }
    }

    private void logListenerError(Module module, String methodName, Exception e) {
        logger.error("Exception in listener method \"{}\" in module \"{}\": {}", methodName, module.getName(), e.getMessage(), e);
    }

    private boolean isReady() {
        return client.player != null && client.player.isLoaded();
    }

    private static class Holder {
        private static final EventManager instance = new EventManager();
    }
}
