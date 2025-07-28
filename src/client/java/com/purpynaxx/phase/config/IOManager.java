package com.purpynaxx.phase.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.purpynaxx.phase.Phase;
import com.purpynaxx.phase.modules.Module;
import com.purpynaxx.phase.modules.Modules;
import com.purpynaxx.phase.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * IOManager is responsible for managing configuration files, including saving and loading data
 * in NBT format, and handling module-specific configurations.
 */
public final class IOManager {

    private static final Logger logger = LoggerFactory.getLogger("Phase/IOManager");
    private static final String ROOT = "";
    private static final Path BASE_DIR = Path.of(MinecraftClient.getInstance().runDirectory.getPath(), "phase");
    private static final Path MODULES_DIR = BASE_DIR.resolve("modules");
    private static final Modules modules = Modules.getInstance();

    private IOManager() {}

    /**
     * Resolves a config file path within the default mod config directory.
     * Creates the directory and any necessary parent directories if they don't exist.
     *
     * @param path The path of the file
     * @return The File object representing the file.
     */
    public static File getFile(Path path) {
        URI uri = BASE_DIR.resolve(path).toUri();
        File configFile = new File(uri);
        File parentDir = configFile.getParentFile();

        if (parentDir != null && !parentDir.exists())
            if (!parentDir.mkdirs())
                logger.error("Could not create parent directories for config file: {}", parentDir.getAbsolutePath());

        return configFile;
    }

    /**
     * Saves data to an NBT file using the provided Codec.
     * The data is wrapped in a root NbtCompound with a specific key.
     *
     * @param path  The path of the file to save to.
     * @param codec The Codec for the data type T.
     * @param data  The data to save.
     * @param <T>   The type of the data.
     */
    public static <T> void saveData(Path path, Codec<T> codec, T data) {

        File nbtFile = ensureNbtFile(getFile(path));

        DataResult<NbtElement> result = codec.encodeStart(NbtOps.INSTANCE, data);
        Optional<NbtElement> nbtElementOptional = Optional.of(result.resultOrPartial(errorMsg -> logger.error("Failed to encode data to NBT for file {}: {}", nbtFile.getName(), errorMsg)).orElseThrow());

        NbtElement encodedElement = nbtElementOptional.get();
        NbtCompound rootCompound = new NbtCompound();
        rootCompound.put(ROOT, encodedElement);

        try {
            NbtIo.write(rootCompound, nbtFile.toPath());
        } catch (IOException e) {
            logger.error("Failed to write data to config file: {}", nbtFile.getAbsolutePath(), e);
        }
    }

    /**
     * Ensures the file has the ".nbt" extension.
     * If the file does not have an extension, it appends ".nbt".
     *
     * @param file The file to ensure the extension for.
     * @return A new File object with the ensured ".nbt" extension.
     */
    private static File ensureNbtFile(File file) {
        // Inexplicitly ensure the extension is set to ".nbt"
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        return new File(file.getParent(), baseName + ".nbt");
    }

    /**
     * Loads data from an NBT file using the provided Codec.
     * It expects the data to be wrapped in a root NbtCompound with a specific key.
     *
     * @param <T>             The type of the data.
     * @param path            The path of the file to load from.
     * @param codec           The Codec for the data type T.
     * @param defaultSupplier A supplier for default data if the file doesn't exist or is corrupt.
     * @return The loaded data, or default data if loading fails or file doesn't exist.
     */
    public static <T> T loadData(Path path, Codec<T> codec, Supplier<T> defaultSupplier) {

        File nbtFile = ensureNbtFile(getFile(path));

        if (!nbtFile.exists()) {
            if (Phase.IS_DEV_ENVIRONMENT) {
                logger.warn("The specified file \"{}\" has not been found, using default data.", nbtFile.getName());
            }
            return defaultSupplier.get();
        }

        try {
            NbtCompound rootCompound = NbtIo.read((nbtFile.toPath()));

            if (rootCompound != null && rootCompound.contains(ROOT)) {
                NbtElement encodedElement = rootCompound.get(ROOT);
                DataResult<T> result = codec.parse(NbtOps.INSTANCE, encodedElement);
                Optional<T> loadedDataOptional = Optional.of(result.resultOrPartial(errorMsg -> logger.error("Failed to parse data from config file {}: {}", nbtFile.getName(), errorMsg)).orElseThrow());
                return loadedDataOptional.get();
            } else {
                logger.warn("Config file {} does not contain the expected root key '{}', using default data.", nbtFile.getName(), ROOT);
                return defaultSupplier.get();
            }
        } catch (IOException e) {
            logger.error("Failed to read config file {}:", nbtFile.getAbsolutePath(), e);
            return defaultSupplier.get();
        } catch (Exception e) {
            logger.error("Unexpected error loading data from {}:", nbtFile.getAbsolutePath(), e);
            return defaultSupplier.get();
        }
    }

    /**
     * Initiates the shutdown process, saving all module configurations.
     */
    public static void shutdown(MinecraftClient ignored) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            executorService.submit(IOManager::saveAllModules).get();
        } catch (Exception e) {
            logger.error("Error while saving module configurations during shutdown", e);
        }
    }

    /**
     * Saves the configuration for all registered modules.
     * This operation runs on a virtual thread.
     */
    private static void saveAllModules() {
        Thread.currentThread().setName("IOManager/saveAllModules");

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

                Path path = MODULES_DIR.resolve(module.getName().toLowerCase());
                IOManager.saveData(path, ModuleConfig.CODEC, config);
            } catch (Exception e) {
                logger.error("Failed to save configuration for module: {}", module.getName(), e);
            }
        }

        logger.info("Saved all module configurations in {}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * Loads configurations for all registered modules.
     * It attempts to load each module's configuration and logs the outcome.
     */
    public static void loadAllModules() {
        int loadedCount = 0;
        for (Module module : modules.getModules())
            if (loadModule(module)) loadedCount++;
        if (loadedCount > 0)
            logger.info("Loaded {} module configurations", loadedCount);
        else logger.warn("Using default configuration");
    }

    /**
     * Loads the configuration for a specific module.
     *
     * @param module The module for which to load the configuration.
     * @return true if the configuration was loaded successfully, false if default data was used or an error occurred.
     */
    public static boolean loadModule(Module module) {
        try {
            AtomicBoolean usingDefault = new AtomicBoolean(false);
            Path path = MODULES_DIR.resolve(module.getName().toLowerCase());

            ModuleConfig config = IOManager.loadData(path, ModuleConfig.CODEC, () -> {
                usingDefault.set(true);
                return new ModuleConfig(module.getName(), new HashMap<>());
            });

            // Apply the loaded configuration
            applyModuleConfig(module, config);

            if (usingDefault.get()) {
                return false;
            } else if (Phase.IS_DEV_ENVIRONMENT) {
                logger.info("Loaded configuration for module: {}", module.getName());
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to load configuration for module: {}", module.getName(), e);
            return false;
        }
    }

    /**
     * Applies a loaded configuration to a module.
     * It iterates through the setting values in the configuration and attempts to apply them to the module's settings.
     *
     * @param module The module to which the configuration should be applied.
     * @param config The ModuleConfig containing the setting values.
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
        }
    }

    /**
     * Applies a value to a setting with type safety.
     * This method handles various primitive types, Strings, Booleans, and Enums.
     *
     * @param setting The setting to which the value should be applied.
     * @param value   The value to apply.
     * @param <T>     The type of the setting's value.
     */
    @SuppressWarnings("unchecked")
    private static <T> void applySetting(Setting<T> setting, Object value) {
        if (value == null) return;

        try {
            Class<T> type = setting.getType();

            if (type.isEnum()) {
                try {
                    // Assuming value is a String representing the ordinal
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
                // Handle different number types explicitly
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
                    return;
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
     * Represents the configuration data for a single module.
     * This record includes the module's name and a map of its setting values.
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