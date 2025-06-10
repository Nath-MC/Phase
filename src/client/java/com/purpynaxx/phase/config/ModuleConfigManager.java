package com.purpynaxx.phase.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.purpynaxx.phase.modules.impl.Module;
import com.purpynaxx.phase.modules.impl.ModuleManager;
import com.purpynaxx.phase.settings.Setting;
import org.jetbrains.annotations.UnmodifiableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleConfigManager.class);
    private static final String MODULES_DIR = "modules";
    private static final ModuleManager moduleManager = ModuleManager.getInstance();
    public static final @UnmodifiableView Set<Module> modules = moduleManager.getModules();

    public static void saveAllModules() {
        long startTime = System.currentTimeMillis();
        for (Module module : modules)
            try {
                Map<String, Object> settingValues = new HashMap<>();
                List<Setting<?>> settings = module.getSettings();

                for (Setting<?> setting : settings)
                    settingValues.put(setting.getName(), setting.getValue());

                ModuleConfig config = new ModuleConfig(module.getName(), module.isActive(), settingValues);

                String filename = MODULES_DIR + "/" + module.getName().toLowerCase().replace(" ", "_");
                File configFile = ConfigManager.getConfigFile(filename);
                ConfigManager.saveData(configFile, ModuleConfig.CODEC, config);
            } catch (Exception e) {
                logger.error("Failed to save configuration for module: {}", module.getName(), e);
            }
        logger.info("Saved all module configurations in {}ms", System.currentTimeMillis() - startTime);
    }

    public static void loadAllModules() {
        int loadedCount = 0;
        for (Module module : modules)
            if (loadModule(module)) loadedCount++;
        logger.info("Loaded {} module configurations", loadedCount);
    }

    public static boolean loadModule(Module module) {
        try {
            String filename = MODULES_DIR + "/" + module.getName().toLowerCase();
            File configFile = ConfigManager.getConfigFile(filename);

            if (!configFile.exists()) {
                logger.debug("No configuration file found for module: {}", module.getName());
                return false;
            }

            ModuleConfig defaultConfig = new ModuleConfig(module.getName(), false, new HashMap<>());
            ModuleConfig config = ConfigManager.loadData(configFile, ModuleConfig.CODEC, () -> defaultConfig);

            // Apply the loaded configuration
            applyModuleConfig(module, config);

            logger.debug("Loaded configuration for module: {}", module.getName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to load configuration for module: {}", module.getName(), e);
            return false;
        }
    }

    /**
     * Applies a loaded configuration to a module
     */
    private static void applyModuleConfig(Module module, ModuleConfig config) {
        for (Map.Entry<String, Object> entry : config.settingValues().entrySet()) {
            String settingName = entry.getKey();
            Object value = entry.getValue();

            Setting<?> setting = moduleManager.getSetting(module, settingName);
            if (setting != null) {
                applySetting(setting, value);
            } else {
                logger.warn("Setting '{}' not found in module '{}'", settingName, module.getName());
            }
        }
        module.setActive(config.active());
    }

    /**
     * Applies a value to a setting with type safety
     */
    @SuppressWarnings("unchecked")
    private static <T> void applySetting(Setting<T> setting, Object value) {
        try {
            if (setting.getValue() != null && setting.getValue().getClass().isInstance(value))
                setting.setValue((T) value);
            else
                logger.warn("Type mismatch for setting '{}': expected {}, got {}", setting.getName(), setting.getValue().getClass().getSimpleName(), value.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Failed to apply value to setting '{}': {}", setting.getName(), e.getMessage());
        }
    }

    /**
     * Represents the configuration data for a single module
     */
    public record ModuleConfig(String moduleName, boolean active, Map<String, Object> settingValues) {
        public static final Codec<ModuleConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("moduleName").forGetter(ModuleConfig::moduleName),
                Codec.BOOL.fieldOf("active").forGetter(ModuleConfig::active),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("settingValues").forGetter(config -> {
                    Map<String, String> stringMap = new HashMap<>();
                    config.settingValues().forEach((key, value) -> stringMap.put(key, value.toString()));
                    return stringMap;
                })
        ).apply(instance, (name, active, settingMap) -> {
            Map<String, Object> objectMap = new HashMap<>(settingMap);
            return new ModuleConfig(name, active, objectMap);
        }));

        public ModuleConfig(String moduleName, boolean active, Map<String, Object> settingValues) {
            this.moduleName = moduleName;
            this.active = active;
            this.settingValues = new HashMap<>(settingValues);
        }
    }
}