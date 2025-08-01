package com.purpynaxx.phase.modules;

import com.purpynaxx.phase.io.IOManager;
import com.purpynaxx.phase.settings.Setting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;

import static com.purpynaxx.phase.Phase.IS_DEV_ENVIRONMENT;

public class Modules {

    private static final Logger LOGGER = LoggerFactory.getLogger("Phase/Modules");
    private static final Modules INSTANCE = new Modules();
    private static final Categories categories = Categories.getInstance();
    private final Map<Class<? extends Module>, Module> modules = new HashMap<>();
    private boolean registered;

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
                    Constructor<? extends Module> constructor = moduleClass.getDeclaredConstructor();
                    constructor.setAccessible(true);

                    Module module = constructor.newInstance();

                    this.modules.put(moduleClass, module);
                    categories.submit(module);

                    if (IS_DEV_ENVIRONMENT) {
                        LOGGER.info("{} has been registered.", moduleClass.getSimpleName());
                    }

                } catch (Exception e) {
                    String message = String.format("Failed to instantiate module: %s", moduleClass.getSimpleName());
                    LOGGER.error(message, e);
                }

            }

            try {
                IOManager.loadAllModules();
            } catch (Exception e) {
                LOGGER.error("Failed to load module configurations", new RuntimeException(e));
            }

        } else {
            LOGGER.warn("No module have been discovered.");
        }

        this.registered = true;
    }

    public @UnmodifiableView Collection<Module> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public <T extends Module> boolean isModuleActive(Class<T> clazz) {
        if (clazz == null) return false;
        Optional<T> optional = getModule(clazz);
        return optional.map(Module::isActive).orElse(false);

    }

    public <T extends Module> Optional<T> getModule(@NotNull Class<T> clazz) {
        return Optional.ofNullable(clazz.cast(modules.get(clazz)));
    }

    public <T extends Module> void toggleModule(Class<T> clazz) {
        if (clazz == null) return;
        Optional<T> optional = getModule(clazz);
        optional.ifPresent(Module::toggle);
    }

    public Optional<Setting<?>> getSetting(Module module, String id) {
        if (module == null || id == null || id.isEmpty()) return null;

        return module.getSettings()
                     .stream()
                     .filter(s -> s.getId().equalsIgnoreCase(id))
                     .findFirst();
    }

}
