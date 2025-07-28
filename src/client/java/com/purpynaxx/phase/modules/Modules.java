package com.purpynaxx.phase.modules;

import com.purpynaxx.phase.config.IOManager;
import com.purpynaxx.phase.settings.Setting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

import static com.purpynaxx.phase.Phase.IS_DEV_ENVIRONMENT;
import static com.purpynaxx.phase.Phase.LOGGER;
import static java.lang.reflect.Modifier.isAbstract;

public class Modules {

    private static final Modules INSTANCE = new Modules();

    private static final Categories categories = Categories.getInstance();

    private final Set<Module> modules = new HashSet<>();

    private final Map<Class<? extends Module>, Module> moduleByClassMap = new HashMap<>();

    private boolean registered;

    private int failedInstantiation;

    private Modules() {}

    public static Modules getInstance() {
        return INSTANCE;
    }

    public void init() {
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
                    add(moduleClass, module);
                } catch (Exception e) {
                    onFail(moduleClass.getSimpleName(), e);
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
                IOManager.loadAllModules();
            } catch (Exception e) {
                LOGGER.error("Failed to load module configurations", new RuntimeException(e));
            }
        } else LOGGER.error("No module classes found.");

        this.registered = true;
    }

    private void add(Class<? extends Module> clazz, Module m) {
        this.moduleByClassMap.put(clazz, m);
        this.modules.add(m);
        categories.submit(m);
        if (IS_DEV_ENVIRONMENT) LOGGER.info("{} has been registered.", clazz.getSimpleName());
    }

    private void onFail(String name, Exception e) {
        String message = String.format("Failed to instantiate module: %s", name);
        LOGGER.error(message, e);
        this.failedInstantiation++;
    }

    public @UnmodifiableView Set<Module> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public <T extends Module> T getModule(@NotNull Class<T> clazz) {
        return clazz.cast(moduleByClassMap.get(clazz));
    }

    public <T extends Module> boolean isModuleActive(Class<T> clazz) {
        if (clazz == null) return false;
        T module = this.getModule(clazz);
        if (module != null) return module.isActive();
        return false;
    }

    public <T extends Module> void toggleModule(Class<T> clazz) {
        if (clazz == null) return;
        T module = this.getModule(clazz);
        if (module != null) module.toggle();
    }

    public @Nullable Setting<?> getSetting(Module module, String id) {
        if (module == null || id == null || id.isEmpty()) return null;

        Optional<Setting<?>> setting = module.getSettings()
                .stream()
                .filter(s -> s.getId().equalsIgnoreCase(id))
                .findFirst();

        return setting.orElse(null);
    }

}