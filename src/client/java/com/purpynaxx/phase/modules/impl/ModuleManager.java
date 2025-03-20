package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.Phase;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.purpynaxx.phase.Phase.logger;

public class ModuleManager {

    private final Set<ModuleBase> modules = new HashSet<>();
    private final Map<Class<? extends ModuleBase>, ModuleBase> classModuleBaseMap = new HashMap<>();
    private boolean registered = false;
    private int failedInstantiation = 0;

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        return Holder.INSTANCE;
    }

    public boolean init() {
        if (registered)
            throw new IllegalStateException("Modules have already been instanced");

        Reflections reflections = new Reflections("com.purpynaxx.phase.modules");
        Set<Class<? extends ModuleBase>> moduleClasses = reflections.getSubTypesOf(ModuleBase.class);

        for (Class<? extends ModuleBase> c : moduleClasses) {
            try {
                Method getInstanceMethod = c.getDeclaredMethod("getInstance");
                getInstanceMethod.setAccessible(true);
                ModuleBase module = (ModuleBase) getInstanceMethod.invoke(null);
                if (!Objects.isNull(module))
                    add(c, module);
                else
                    onFail(c.getSimpleName(), new NullPointerException("Module instance is null"));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                onFail(c.getSimpleName(), e);
            }
        }

        if (failedInstantiation == 0) logger.info("{} modules were instanced.", modules.size());
        else logger.info("{} modules were instanced. {} failed.", modules.size(), failedInstantiation);
        registered = true;
        return failedInstantiation == 0; // True if all modules instanced correctly, false otherwise
    }

    private void add(Class<? extends ModuleBase> clazz, ModuleBase m) {
        classModuleBaseMap.put(clazz, m);
        modules.add(m);
        if (Phase.isDevEnvironment) Phase.logger.info("{} has been registered.", clazz.getSimpleName());
    }

    private void onFail(String name, Exception e) {
        Phase.logger.error("Failed to instantiate {} : {}", name, e);
        failedInstantiation++;
    }

    public Set<ModuleBase> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public <T extends ModuleBase> T getModuleByClass(@NotNull Class<T> clazz) {
        return clazz.cast(classModuleBaseMap.get(clazz));
    }

    public boolean isModuleActive(Class<? extends ModuleBase> clazz) {
        return this.isModuleActive(this.classModuleBaseMap.get(clazz));
    }

    public boolean isModuleActive(@NotNull ModuleBase module) {
        return module.isActive();
    }

    // Using an inner static class for lazy, thread-safe initialization
    private static class Holder {
        private static final ModuleManager INSTANCE = new ModuleManager();
    }

}
