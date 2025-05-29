package com.purpynaxx.phase.modules.impl;

import com.purpynaxx.phase.settings.Setting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.purpynaxx.phase.Phase.isDevEnvironment;
import static com.purpynaxx.phase.Phase.logger;
import static java.lang.reflect.Modifier.isAbstract;

public final class ModuleManager {

    private final Set<Module> modules = new HashSet<>();
    private final Set<Module.Category> categories = new HashSet<>();
    private final Map<Class<? extends Module>, Module> classModuleBaseMap = new HashMap<>();
    private final Map<Module.Category, Set<Module>> modulesByCategoryMap = new HashMap<>();
    private final Map<Class<? extends Module>, List<Setting<?>>> moduleSettings = new HashMap<>();
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

        if (moduleClasses.isEmpty()) logger.error("No module classes found.");

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

                this.moduleSettings.putIfAbsent(moduleClass, new ArrayList<>());
                Field[] fields = moduleClass.getFields();

                for (Field field : fields) {
                    if (Setting.class.isAssignableFrom(field.getType())) {
                        Setting<?> setting = (Setting<?>) field.get(module);
                        this.moduleSettings.get(moduleClass).add(setting);
                    }
                }
            } catch (NoSuchMethodException e) {
                this.onFail(moduleClass.getSimpleName(), new Exception("No default constructor found", e));
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                this.onFail(moduleClass.getSimpleName(), e);
            }
        }

        if (this.failedInstantiation == 0)
            logger.info("{} modules were successfully initialized.", this.modules.size());
        else logger.info("{} modules were initialized. {} failed.", this.modules.size(), this.failedInstantiation);

        this.registered = true;
        return this.failedInstantiation == 0;
    }

    private void add(Class<? extends Module> clazz, Module m) {
        this.classModuleBaseMap.put(clazz, m);
        this.modules.add(m);
        this.categories.add(m.getCategory());
        if (isDevEnvironment) logger.info("{} has been registered.", clazz.getSimpleName());
    }

    private void onFail(String name, Exception e) {
        logger.error("Failed to instantiate {} : {}", name, e);
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

    public <T extends Module> void toggleModuleActive(Class<T> clazz) {
        if (clazz == null) return;
        T module = this.getModuleByClass(clazz);
        if (module != null) module.toggle();
    }

    public @Nullable Setting<?> getSetting(Module module, String name) {
        if (module == null || name == null || name.isEmpty()) return null;
        List<Setting<?>> settings = this.moduleSettings.get(module.getClass());
        if (settings == null || settings.isEmpty()) return null;

        for (Setting<?> setting : settings)
            if (setting.getName().equalsIgnoreCase(name))
                return setting;

        return null;
    }

    public @UnmodifiableView List<Setting<?>> getSettings(Module module) {
        if (module == null) return Collections.emptyList();
        Class<? extends Module> moduleClass = module.getClass();
        List<Setting<?>> settings = this.moduleSettings.get(moduleClass);
        return Collections.unmodifiableList(settings);
    }

    private static class Holder {
        private static final ModuleManager INSTANCE = new ModuleManager();
    }
}
