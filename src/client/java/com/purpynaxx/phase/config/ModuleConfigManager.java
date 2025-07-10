package com.purpynaxx.phase.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.purpynaxx.phase.modules.Module;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ModuleConfigManager {

    private static final Logger logger = ConfigManager.logger;
    private static final String modules_directory = "modules";
    private static final Modules modules = Modules.getInstance();

    public static void shutdown(MinecraftClient ignored) {
        Executors.newVirtualThreadPerTaskExecutor().submit(ModuleConfigManager::saveAllModules);
    }

    private static void saveAllModules() {
        Thread.currentThread().setName("ModuleConfigManager/saveAllModules");

        long startTime = System.currentTimeMillis();

        for (Module module : modules.getModules()) {
            try {
                Map<String, Object> settingValues = new HashMap<>();
                List<Setting<?>> settings = module.getSettings();

                for (Setting<?> setting : settings) {
                    if (setting.getType().isEnum()) {
                        settingValues.put(setting.getId(), ((Enum<?>) setting.getValue()).ordinal());
                        continue;
                    }
                    settingValues.put(setting.getId(), setting.getValue());
                }

                ModuleConfig config = new ModuleConfig(module.getName(), settingValues);

                String filename = modules_directory + "/" + module.getName().toLowerCase().replace(" ", "_");
                File configFile = ConfigManager.getConfigFile(filename);
                ConfigManager.saveData(configFile, ModuleConfig.CODEC, config);
            } catch (Exception e) {
                logger.error("Failed to save configuration for module: {}", module.getName(), e);
            }
        }

        logger.info("Saved all module configurations in {}ms", System.currentTimeMillis() - startTime);
    }

    public static void loadAllModules() {
        int loadedCount = 0;
        for (Module module : modules.getModules())
            if (loadModule(module)) loadedCount++;
        if (loadedCount > 0)
            logger.info("Loaded {} module configurations", loadedCount);
        else logger.warn("Using default configuration");
    }

    public static boolean loadModule(Module module) {
        try {
            String filename = modules_directory + "/" + module.getName().toLowerCase();
            File configFile = ConfigManager.getConfigFile(filename);

            if (!configFile.exists()) {
                logger.debug("No configuration file found for module: {}", module.getName());
                return false;
            }

            ModuleConfig defaultConfig = new ModuleConfig(module.getName(), new HashMap<>());
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
            String settingId = entry.getKey();
            Object value = entry.getValue();

            Setting<?> setting = modules.getSetting(module, settingId);
            if (setting != null) {
                applySetting(setting, value);
            } else {
                logger.warn("Setting '{}' not found in module '{}'", settingId, module.getName());
            }
            module.triggerEvents();
        }
    }

    /**
     * Applies a value to a setting with type safety
     */
    @SuppressWarnings("unchecked")
    private static <T> void applySetting(Setting<T> setting, Object value) {
        if (value == null) return;

        try {
            Class<T> type = setting.getType();

            if (type.isEnum()) {
                try {
                    int ordinal = Integer.parseInt((String) value);
                    T enumValue = Arrays.stream(type.getEnumConstants()).toList().get(ordinal);
                    setting.setValue(enumValue);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid enum value '{}' for enum type {}", value, type.getName());
                }
                return;
            }

            if (type.isAssignableFrom(String.class)) {
                setting.setValue((T) value);
                return;
            }

            if (type.isAssignableFrom(Boolean.class)) {
                setting.setValue((T) Boolean.valueOf((String) value));
                return;
            }

            if (type.isAssignableFrom(Number.class)) {
                if (type.isAssignableFrom(Byte.class)) {
                    setting.setValue((T) Byte.valueOf(value.toString()));
                    return;
                }

                if (type.isAssignableFrom(Short.class)) {
                    setting.setValue((T) Short.valueOf(value.toString()));
                    return;
                }

                if (type.isAssignableFrom(Integer.class)) {
                    setting.setValue((T) Integer.valueOf(value.toString()));
                    return;
                }

                if (type.isAssignableFrom(Float.class)) {
                    setting.setValue((T) Float.valueOf(value.toString()));
                    return;
                }


                if (type.isAssignableFrom(Double.class)) {
                    setting.setValue((T) Double.valueOf(value.toString()));
                    return;
                }

                if (type.isAssignableFrom(Long.class)) {
                    setting.setValue((T) Long.valueOf(value.toString()));
                }
            }

            logger.error("Type mismatch for setting '{}': expected {}, got {}",
                    setting.getName(),
                    setting.getValue().getClass().getSimpleName(),
                    value.getClass().getSimpleName());

        } catch (Exception e) {
            logger.error("Failed to apply value to setting '{}': {}", setting.getName(), e.getMessage());
        }
    }

    /**
     * Represents the configuration data for a single module
     */
    record ModuleConfig(String moduleName, Map<String, Object> settingValues) {

        public static final Codec<ModuleConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("moduleName").forGetter(ModuleConfig::moduleName),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("settingValues").forGetter(config -> {
                    Map<String, String> stringMap = new HashMap<>();
                    config.settingValues().forEach((key, value) -> stringMap.put(key, value.toString()));
                    return stringMap;
                })
        ).apply(instance, (name, settingMap) -> {
            Map<String, Object> objectMap = new HashMap<>(settingMap);
            return new ModuleConfig(name, objectMap);
        }));

        public ModuleConfig(String moduleName, Map<String, Object> settingValues) {
            this.moduleName = moduleName;
            this.settingValues = new HashMap<>(settingValues);
        }

    }

}