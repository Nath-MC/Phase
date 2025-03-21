package com.purpynaxx.phase.events;

import com.purpynaxx.phase.events.annotations.Event;
import com.purpynaxx.phase.events.network.PacketCallback;
import com.purpynaxx.phase.modules.impl.ModuleBase;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import static com.purpynaxx.phase.Phase.isDevEnvironment;
import static com.purpynaxx.phase.Phase.logger;
import static com.purpynaxx.phase.helpers.Player.isPlayerInWorld;

public class EventManager {

    private final ModuleManager moduleManager = ModuleManager.getInstance();

    private EventManager() {}

    public static EventManager getInstance() {
        return Holder.instance;
    }

    public void init() {
        Set<ModuleBase> modules = getModules();
        int methodRegistered = 0;

        for (ModuleBase module : modules) {
            Class<? extends ModuleBase> clazz = module.getClass();
            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (!method.isAnnotationPresent(Event.class)) continue;
                Event eventAnnotation = method.getAnnotation(Event.class);
                Type eventType = eventAnnotation.value();

                if (Objects.isNull(eventType)) {
                    logger.warn("Event value not defined in annotation for method \"{}\" in class \"{}\".", method.getName(), clazz.getSimpleName());
                    continue;
                }

                registerEvent(method, module, eventType);
                if (isDevEnvironment) logger.info("Registered method {}.{}", clazz.getSimpleName(), method.getName());
                methodRegistered++;
            }
        }
        logger.info("{} methods were registered in {} classes.", methodRegistered, modules.size());
    }

    private void registerEvent(Method method, ModuleBase module, @NotNull Type eventType) {
        switch (eventType) {
            case onStartWorldTick -> ClientTickEvents.START_WORLD_TICK.register(world -> invoke(method, module, world));
            case onEndWorldTick -> ClientTickEvents.END_CLIENT_TICK.register(world -> invoke(method, module, world));
            case onStartClientTick -> ClientTickEvents.START_CLIENT_TICK.register(client -> invoke(method, module));
            case onEndClientTick -> ClientTickEvents.END_CLIENT_TICK.register(client -> invoke(method, module));
            case onWorldJoin ->
                    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> invoke(method, module));
            case onWorldLeave ->
                    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> invoke(method, module));
            case onPacketReceive ->
                    PacketCallback.IN.register((packet, event) -> invoke(method, module, packet, event));
            case onPacketSend -> PacketCallback.OUT.register((packet, event) -> invoke(method, module, packet, event));
        }
    }

    private void invoke(Method method, ModuleBase module, Object... args) {
        if (moduleManager.isModuleActive(module) && areArgsValid(method, args) && isPlayerInWorld()) {
            try {
                method.setAccessible(true);
                method.invoke(module, args);
            } catch (InvocationTargetException | IllegalAccessException e) {
                logger.error("Could not invoke method \"{}\" in class \"{}\" : {}", method.getName(), module.getName(), e);
            }
        }
    }

    private boolean areArgsValid(@NotNull Method method, Object @NotNull ... args) {
        @NotNull Class<?>[] expectedTypes = method.getParameterTypes();
        int argsCount = expectedTypes.length;
        if (args.length != argsCount) return false;
        for (int i = 0; i < argsCount; i++)
            if (args[i] == null || !expectedTypes[i].isAssignableFrom(args[i].getClass())) return false;
        return true;
    }

    private @Unmodifiable Set<ModuleBase> getModules() {
        return moduleManager.getModules();
    }

    public enum Type {
        onStartWorldTick,
        onEndWorldTick,
        onStartClientTick,
        onEndClientTick,
        onPacketReceive,
        onPacketSend,
        onWorldJoin,
        onWorldLeave
    }

    private static class Holder {
        private static final EventManager instance = new EventManager();
    }

}
