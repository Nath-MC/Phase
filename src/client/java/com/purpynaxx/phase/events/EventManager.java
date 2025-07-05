package com.purpynaxx.phase.events;

import com.purpynaxx.phase.config.ModuleConfigManager;
import com.purpynaxx.phase.events.interfaces.client.ClientTick;
import com.purpynaxx.phase.events.interfaces.network.PacketHandler;
import com.purpynaxx.phase.events.interfaces.world.WorldConnectivity;
import com.purpynaxx.phase.events.interfaces.world.WorldRender;
import com.purpynaxx.phase.events.interfaces.world.WorldTick;
import com.purpynaxx.phase.events.network.PacketEvent;
import com.purpynaxx.phase.helpers.render.Renderer;
import com.purpynaxx.phase.mixins.accessors.TitleScreenMixin;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.modules.impl.Modules;
import com.purpynaxx.phase.modules.visuals.GUI;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
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

    private static final Modules modules = Modules.getInstance();

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

    private static final Set<Class<? extends Screen>> ignoredScreens = Set.of(
            MessageScreen.class,
            LevelLoadingScreen.class,
            ProgressScreen.class,
            DownloadingTerrainScreen.class
    );

    private static final Renderer renderer = Renderer.getInstance();

    private EventManager() {}

    public static EventManager getInstance() {
        return Holder.INSTANCE;
    }

    public void init() {
        registerModuleListeners();
        registerGlobalEvents();
    }

    private void registerModuleListeners() {
        int listenersRegistered = 0;
        int modulesScanned = 0;

        for (Module module : modules.getModules()) {
            modulesScanned++;
            int moduleRegistered = 0;

            moduleRegistered += registerListener(module, WorldTick.BEFORE.class,
                    listener -> ClientTickEvents.START_WORLD_TICK.register(world -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.beforeWorldTick(world), "beforeWorldTick", module);
                        }
                    }));

            moduleRegistered += registerListener(module, WorldTick.AFTER.class,
                    listener -> ClientTickEvents.END_WORLD_TICK.register(world -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.afterWorldTick(world), "afterWorldTick", module);
                        }
                    }));

            moduleRegistered += registerListener(module, ClientTick.BEFORE.class,
                    listener -> ClientTickEvents.START_CLIENT_TICK.register(client -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.beforeClientTick(client), "beforeClientTick", module);
                        }
                    }));

            moduleRegistered += registerListener(module, ClientTick.AFTER.class,
                    listener -> ClientTickEvents.END_CLIENT_TICK.register(client -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.afterClientTick(client), "afterClientTick", module);
                        }
                    }));

            moduleRegistered += registerListener(module, WorldConnectivity.JOIN.class,
                    listener -> ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                        if (module.isActive()) {
                            safelyExecute(() -> listener.onWorldJoin(handler, sender, client), "onWorldJoin", module);
                        }
                    }));

            moduleRegistered += registerListener(module, WorldConnectivity.LEAVE.class,
                    listener -> ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                        if (module.isActive()) {
                            safelyExecute(() -> listener.onWorldLeave(handler, client), "onWorldLeave", module);
                        }
                    }));

            moduleRegistered += registerListener(module, PacketHandler.IN.class,
                    listener -> PacketEvent.IN.register((packet, event) -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onPacketReceive(packet, event), "onPacketReceive", module);
                        }
                    }));

            moduleRegistered += registerListener(module, PacketHandler.OUT.class,
                    listener -> PacketEvent.OUT.register((packet, event) -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> listener.onPacketSend(packet, event), "onPacketSend", module);
                        }
                    }));

            moduleRegistered += registerListener(module, WorldRender.END.class,
                    listener -> WorldRenderEvents.END.register(context -> {
                        if (module.isActive() && isReady()) {
                            safelyExecute(() -> Renderer.positionMatrixAndRender(context, () -> listener.onWorldRenderEnd(context)), "onWorldRenderEnd", module);
                        }
                    }));

            listenersRegistered += moduleRegistered;

            if (moduleRegistered == 0 && IS_DEV_ENVIRONMENT) {
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
            logRegistration(module, listenerClass);
            return 1;
        }
        return 0;
    }

    private void safelyExecute(Runnable action, String methodName, Module module) {
        try {
            action.run();
        } catch (Exception e) {
            logListenerError(module, methodName, e);
            module.setActive(false); // kill switch
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
                    && modules.isModuleActive(GUI.class)) {
                ((TitleScreenMixin) titleScreen).setDoBackgroundFade(false);
            }

            if (ignoredScreens.contains(screen.getClass())) return;

            ScreenKeyboardEvents.beforeKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (screen.getFocused() instanceof TextFieldWidget) return;
                if (keyBinding.matchesKey(key, scancode)) {
                    modules.toggleModuleActive(GUI.class);
                }
            });
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            // Register tick event for key binding
            if (keyBinding.wasPressed()) {
                modules.toggleModuleActive(GUI.class);
            }

            // Handle renderer tick
            renderer.tick();

        });


        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> {
            // Invalidate renderer queue on disconnect
            renderer.clearQueue();
        });


        WorldRenderEvents.END.register(worldRenderContext -> Renderer.positionMatrixAndRender(worldRenderContext, () -> renderer.renderQueue(worldRenderContext)));


        // Register shutdown event
        ClientLifecycleEvents.CLIENT_STOPPING.register(ModuleConfigManager::shutdown);
    }

    private void logRegistration(Module module, Class<?> listenerClass) {
        if (!IS_DEV_ENVIRONMENT) return;

        String completeName = listenerClass.getDeclaringClass().getSimpleName() + "." + listenerClass.getSimpleName();
        logger.info("Subscribed {} to {}", module.getName(), completeName);

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