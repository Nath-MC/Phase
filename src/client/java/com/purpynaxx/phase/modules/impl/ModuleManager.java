package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.config.ModuleConfigManager;
import com.purpynaxx.phase.settings.Setting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.purpynaxx.phase.Phase.IS_DEV_ENVIRONMENT;
import static com.purpynaxx.phase.Phase.LOGGER;
import static java.lang.reflect.Modifier.isAbstract;

public final class ModuleManager {

    private final Set<Module> modules = new HashSet<>();
    private final Set<Module.Category> categories = new HashSet<>();
    private final Map<Class<? extends Module>, Module> classModuleBaseMap = new HashMap<>();
    private final Map<Module.Category, Set<Module>> modulesByCategoryMap = new HashMap<>();
    private boolean registered;
    private int failedInstantiation;

    private ModuleManager() {}

    public static ModuleManager getInstance() {
        return Holder.INSTANCE;
    }

    public @UnmodifiableView Map<Module.Category, Set<Module>> getModulesByCategoryMap() {
        return Collections.unmodifiableMap(modulesByCategoryMap);
    }

    public @UnmodifiableView Set<Module.Category> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public boolean init() {
        if (this.registered)
            throw new IllegalStateException("Modules have already been instanced");

        Reflections reflections = new Reflections("com.purpynaxx.phase.modules");
        Set<Class<? extends Module>> moduleClasses = reflections.getSubTypesOf(Module.class);

        if (!moduleClasses.isEmpty()) {

            for (Class<? extends Module> moduleClass : moduleClasses) {
                try {
                    if (isAbstract(moduleClass.getModifiers())) continue;

                    Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Module module = constructor.newInstance();
                    this.add(moduleClass, module);

                    Module.Category category = module.getCategory();
                    this.modulesByCategoryMap.putIfAbsent(category, new HashSet<>());
                    this.modulesByCategoryMap.get(category).add(module);
                } catch (NoSuchMethodException e) {
                    this.onFail(moduleClass.getSimpleName(), new Exception("No default constructor found", e));
                } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    this.onFail(moduleClass.getSimpleName(), e);
                }
            }

            if (IS_DEV_ENVIRONMENT) {
                if (this.failedInstantiation == 0) {
                    LOGGER.info("{} modules were successfully initialized.", this.modules.size());
                } else {
                    LOGGER.info("{} modules were initialized. {} failed.", this.modules.size(), this.failedInstantiation);
                }
            }

            try {
                ModuleConfigManager.loadAllModules();
            } catch (Exception e) {
                LOGGER.error("Failed to load module configurations", new RuntimeException(e));
            }
        } else LOGGER.error("No module classes found.");

        this.registered = true;
        return this.failedInstantiation == 0;
    }

    private void add(Class<? extends Module> clazz, Module m) {
        this.classModuleBaseMap.put(clazz, m);
        this.modules.add(m);
        this.categories.add(m.getCategory());
        if (IS_DEV_ENVIRONMENT) LOGGER.info("{} has been registered.", clazz.getSimpleName());
    }

    private void onFail(String name, Exception e) {
        LOGGER.error("Failed to instantiate {} : {}", name, e);
        this.failedInstantiation++;
    }

    public @UnmodifiableView Set<Module> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public <T extends Module> T getModuleByClass(@NotNull Class<T> clazz) {
        return clazz.cast(classModuleBaseMap.get(clazz));
    }

    public <T extends Module> void setModuleActive(Class<T> clazz, boolean active) {
        if (clazz == null) return;
        T module = this.getModuleByClass(clazz);
        if (module != null) module.setActive(active);
    }

    public <T extends Module> boolean isModuleActive(Class<T> clazz) {
        if (clazz == null) return false;
        T module = this.getModuleByClass(clazz);
        if (module != null) return module.isActive();
        return false;
    }

    public <T extends Module> void toggleModuleActive(Class<T> clazz) {
        if (clazz == null) return;
        T module = this.getModuleByClass(clazz);
        if (module != null) module.toggle();
    }

    public @Nullable Setting<?> getSetting(Module module, String name) {
        if (module == null || name == null || name.isEmpty()) return null;
        List<Setting<?>> settings = module.getSettings();
        for (Setting<?> setting : settings)
            if (setting.getName().equalsIgnoreCase(name))
                return setting;

        return null;
    }

    private static class Holder {

        private static final ModuleManager INSTANCE = new ModuleManager();

    }

}