package com.purpynaxx.phase.modules;

import com.purpynaxx.phase.Phase;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ModuleRegister {

    // Using an inner static class for lazy, thread-safe initialization
    private static class Holder {
        private static final ModuleRegister INSTANCE = new ModuleRegister();
    }

    private final Set<ModuleBase> modules = new HashSet<>();
    private final Map<Class<? extends ModuleBase>, ModuleBase> classModuleBaseMap = new HashMap<>();
    private boolean flawlessInstantiation = true;
    private boolean registered = false;

    private ModuleRegister() {}

    public static ModuleRegister getInstance() {
        return Holder.INSTANCE;
    }

    public boolean register() {

        if (registered) {
            Phase.logger.warn("modules have already been registered; skipping re-registration");
            return flawlessInstantiation;
        }

        Reflections reflections = new Reflections("com.purpynaxx.phase.modules");
        Set<Class<? extends ModuleBase>> moduleClasses = reflections.getSubTypesOf(ModuleBase.class);

        for (Class<? extends ModuleBase> c : moduleClasses) {
            try {
                Method getInstanceMethod = c.getDeclaredMethod("getInstance");
                ModuleBase module = (ModuleBase) getInstanceMethod.invoke(null);
                if (!Objects.isNull(module))
                    add(c, module);
                else
                    onFail(c.getName(), new NullPointerException("Module instance is null"));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                onFail(c.getName(), e);
            }
        }
        registered = true;
        return flawlessInstantiation;
    }

    private void add(Class<? extends ModuleBase> clazz, ModuleBase m) {
        classModuleBaseMap.put(clazz, m);
        modules.add(m);
        if (Phase.isDevEnvironment) Phase.logger.info("{} has been registered.", clazz.getName());
    }

    private void onFail(String name, Exception e) {
        Phase.logger.error("Failed to instantiate {} : {}", name, e);
        flawlessInstantiation = false;
    }

    public Set<ModuleBase> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public <T extends ModuleBase> T getModuleByClass(@NotNull Class<T> clazz) {
        return clazz.cast(classModuleBaseMap.get(clazz));
    }

}
