package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.Phase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.purpynaxx.phase.Phase.logger;

public class ModuleManager {

    private final Set<ModuleBase> modules = new HashSet<>();
    private final Set<Category> categories = new HashSet<>();
    private final Map<Class<? extends ModuleBase>, ModuleBase> classModuleBaseMap = new HashMap<>();
    private final Map<Category, Set<ModuleBase>> modulesByCategoryMap = new HashMap<>();
    private boolean registered;
    private int failedInstantiation;
    private ModuleManager() {}

    public static ModuleManager getInstance() {
        return Holder.INSTANCE;
    }

    public @UnmodifiableView Map<Category, Set<ModuleBase>> getModulesByCategoryMap() {
        return Collections.unmodifiableMap(modulesByCategoryMap);
    }

    public @UnmodifiableView Set<Category> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public boolean init() {
        if (registered)
            throw new IllegalStateException("Modules have already been instanced");

        Reflections reflections = new Reflections("com.purpynaxx.phase.modules");
        Set<Class<? extends ModuleBase>> moduleClasses = reflections.getSubTypesOf(ModuleBase.class);

        for (Class<? extends ModuleBase> c : moduleClasses) {
            try {
                Constructor<? extends ModuleBase> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                ModuleBase module = constructor.newInstance();
                add(c, module);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                     InstantiationException e) {
                onFail(c.getSimpleName(), e);
            }
        }

        for (ModuleBase module : this.getModules()) {
            Category category = module.getCategory();
            modulesByCategoryMap.putIfAbsent(category, new HashSet<>());
            modulesByCategoryMap.get(category).add(module);
        }

        if (failedInstantiation == 0) logger.info("{} modules were instanced.", modules.size());
        else logger.info("{} modules were instanced. {} failed.", modules.size(), failedInstantiation);
        registered = true;
        return failedInstantiation == 0;
    }

    private void add(Class<? extends ModuleBase> clazz, ModuleBase m) {
        classModuleBaseMap.put(clazz, m);
        modules.add(m);
        categories.add(m.getCategory());
        if (Phase.isDevEnvironment) Phase.logger.info("{} has been registered.", clazz.getSimpleName());
    }

    private void onFail(String name, Exception e) {
        Phase.logger.error("Failed to instantiate {} : {}", name, e);
        failedInstantiation++;
    }

    public @UnmodifiableView Set<ModuleBase> getModules() {
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

    public void setModuleActive(Class<? extends ModuleBase> clazz, boolean active) {
        this.setModuleActive(this.classModuleBaseMap.get(clazz), active);
    }

    public void setModuleActive(@NotNull ModuleBase module, boolean active) {
        module.setActive(active);
    }

    public void toggleModuleActive(Class<? extends ModuleBase> clazz) {
        this.toggleModuleActive(this.classModuleBaseMap.get(clazz));
    }

    public void toggleModuleActive(@NotNull ModuleBase module) {
        boolean active = module.isActive();
        this.setModuleActive(module, !active);
    }

    private static class Holder {
        private static final ModuleManager INSTANCE = new ModuleManager();
    }
}
