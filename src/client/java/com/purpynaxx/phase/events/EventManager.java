package com.purpynaxx.phase.events;

import com.purpynaxx.phase.config.ModuleConfigManager;
import com.purpynaxx.phase.events.listeners.*;
import com.purpynaxx.phase.events.network.PacketCallback;
import com.purpynaxx.phase.mixins.accessors.TitleScreenMixin;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import com.purpynaxx.phase.modules.visuals.GUI;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Consumer;

import static com.purpynaxx.phase.Phase.IS_DEV_ENVIRONMENT;

public class EventManager {

    private static final ModuleManager moduleManager = ModuleManager.getInstance();
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private static final Set<Class<? extends Screen>> ignoredScreens = Set.of(
            MessageScreen.class,
            LevelLoadingScreen.class,
            ProgressScreen.class,
            DownloadingTerrainScreen.class
    );

    private EventManager() {}

    public static EventManager getInstance() {
        return Holder.INSTANCE;
    }

    public void init() {
        registerModuleListeners();
        registerGlobalEvents();
    }

    private void registerModuleListeners() {
        Set<Module> modules = moduleManager.getModules();
        int listenersRegistered = 0;
        int modulesScanned = 0;

        for (Module module : modules) {
            modulesScanned++;
            int registeredForModule = 0;

            registeredForModule += registerListener(module, WorldStartTickListener.class,
                    listener -> ClientTickEvents.START_WORLD_TICK.register(world -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onWorldTickStart(world), "onWorldTickStart", module);
                        }
                    }));

            registeredForModule += registerListener(module, WorldTickEndListener.class,
                    listener -> ClientTickEvents.END_WORLD_TICK.register(world -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onWorldTickEnd(world), "onWorldTickEnd", module);
                        }
                    }));

            registeredForModule += registerListener(module, ClientTickStartListener.class,
                    listener -> ClientTickEvents.START_CLIENT_TICK.register(client -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onClientTickStart(client), "onClientTickStart", module);
                        }
                    }));

            registeredForModule += registerListener(module, ClientTickEndListener.class,
                    listener -> ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onClientTickEnd(client), "onClientTickEnd", module);
                        }
                    }));

            registeredForModule += registerListener(module, WorldJoinListener.class,
                    listener -> ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                        if (module.isActive()) {
                            safelyExecute(() -> listener.onWorldJoin(handler, sender, client), "onWorldJoin", module);
                        }
                    }));

            registeredForModule += registerListener(module, WorldLeaveListener.class,
                    listener -> ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                        if (module.isActive()) {
                            safelyExecute(() -> listener.onWorldLeave(handler, client), "onWorldLeave", module);
                        }
                    }));

            registeredForModule += registerListener(module, PacketReceiveListener.class,
                    listener -> PacketCallback.IN.register((packet, event) -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onPacketReceive(packet, event), "onPacketReceive", module);
                        }
                    }));

            registeredForModule += registerListener(module, PacketSendListener.class,
                    listener -> PacketCallback.OUT.register((packet, event) -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onPacketSend(packet, event), "onPacketSend", module);
                        }
                    }));

            listenersRegistered += registeredForModule;

            if (registeredForModule == 0 && IS_DEV_ENVIRONMENT) {
                logger.warn("Module {} implements no known listener interfaces.", module.getClass().getSimpleName());
            }
        }

        if (IS_DEV_ENVIRONMENT) {
            logger.info("{} listeners were registered across {} scanned modules.", listenersRegistered, modulesScanned);
        }
    }

    private <T> int registerListener(Module module, Class<T> listenerClass, Consumer<T> registrationMethod) {
        if (listenerClass.isInstance(module)) {
            T listener = listenerClass.cast(module);
            registrationMethod.accept(listener);
            logRegistration(module, listenerClass.getSimpleName());
            return 1;
        }
        return 0;
    }

    private void safelyExecute(Runnable action, String methodName, Module module) {
        try {
            action.run();
        } catch (Exception e) {
            logListenerError(module, methodName, e);
        }
    }

    private void registerGlobalEvents() {
        // Register menu key binding
        KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.phase.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.categories.phase"
        ));

        // Register screen events
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen titleScreen && ((TitleScreenMixin) titleScreen).getDoBackgroundFade()
                    && moduleManager.isModuleActive(GUI.class)) {
                ((TitleScreenMixin) titleScreen).setDoBackgroundFade(false);
            }

            if (ignoredScreens.contains(screen.getClass())) return;

            ScreenKeyboardEvents.beforeKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (screen.getFocused() instanceof TextFieldWidget) return;
                if (keyBinding.matchesKey(key, scancode)) {
                    moduleManager.toggleModuleActive(GUI.class);
                }
            });
        });

        // Register tick event for key binding
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.wasPressed()) {
                moduleManager.toggleModuleActive(GUI.class);
            }
        });

        // Register shutdown event
        ClientLifecycleEvents.CLIENT_STOPPING.register(ModuleConfigManager::shutdown);
    }

    private void logRegistration(Module module, String listenerType) {
        if (IS_DEV_ENVIRONMENT) {
            logger.info("Registered {} as {}", module.getName(), listenerType);
        }
    }

    private void logListenerError(Module module, String methodName, Exception e) {
        logger.error("Exception in listener method \"{}\" in module \"{}\": {}",
                methodName, module.getName(), e.getMessage(), e);
    }

    private boolean isReady() {
        return client.player != null && client.player.isLoaded();
    }

    private static class Holder {

        private static final EventManager INSTANCE = new EventManager();

    }

}