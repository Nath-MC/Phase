package com.purpynaxx.phase.io;

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
            logger.error("Failed to write data to file: {}", nbtFile.getAbsolutePath(), e);
        }
    }

    /**
     * Resolves a file path within the default mod config directory.
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
            if (!parentDir.mkdirs()) logger.error("Could not create parent directories for file: {}", parentDir.getAbsolutePath());

        return configFile;
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
     * Saves the configuration for all registered modules.
     * This operation runs on a virtual thread.
     */
    private static void saveAllModules() {
        Thread.currentThread().setName("IOManager/saveAllModules");
        long startTime = System.currentTimeMillis();

        for (Module module : modules.getModules()) {
            try {
                NbtCompound settingsCompound = new NbtCompound();

                for (Setting<?> setting : module.getSettings()) {
                    NbtElement encodedValue = encodeSetting(setting);
                    settingsCompound.put(setting.getId(), encodedValue);
                }

                ModuleConfig config = new ModuleConfig(module.getName(), settingsCompound);
                Path path = MODULES_DIR.resolve(module.getName().toLowerCase());
                IOManager.saveData(path, ModuleConfig.CODEC, config);

            } catch (Exception e) {
                logger.error("Failed to save configuration for module: {}", module.getName(), e);
            }
        }

        logger.info("Saved all module configurations in {}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * Initiates the shutdown process, saving all module configurations.
     */
    public static void shutdown(MinecraftClient ignored) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            executorService.submit(IOManager::saveAllModules);
        }
    }

    private static <T> NbtElement encodeSetting(Setting<T> setting) {
        return setting.getCodec().encodeStart(NbtOps.INSTANCE, setting.getValue()).resultOrPartial(errorMsg -> logger.error("Failed to encode setting '{}' for module '{}': {}", setting.getId(),
                setting.getModule().getName(), errorMsg)).orElse(new NbtCompound());
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
                return new ModuleConfig(module.getName(), new NbtCompound());
            });

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
                logger.warn("The requested file \"{}\" has not been found, using default data.", nbtFile.getName());
            }
            return defaultSupplier.get();
        }

        try {
            NbtCompound rootCompound = NbtIo.read((nbtFile.toPath()));

            if (rootCompound != null && rootCompound.contains(ROOT)) {
                NbtElement encodedElement = rootCompound.get(ROOT);
                DataResult<T> result = codec.parse(NbtOps.INSTANCE, encodedElement);
                Optional<T> loadedDataOptional = Optional.of(result.resultOrPartial(errorMsg -> logger.error("Failed to parse data from file {}: {}", nbtFile.getName(), errorMsg)).orElseThrow());
                return loadedDataOptional.get();
            } else {
                logger.warn("Config file {} does not contain the expected root key '{}', using default data.", nbtFile.getName(), ROOT);
                return defaultSupplier.get();
            }
        } catch (IOException e) {
            logger.error("Failed to read file {}:", nbtFile.getAbsolutePath(), e);
            return defaultSupplier.get();
        } catch (Exception e) {
            logger.error("Unexpected error loading data from {}:", nbtFile.getAbsolutePath(), e);
            return defaultSupplier.get();
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
        NbtCompound settingsCompound = config.settings();

        for (String settingId : settingsCompound.getKeys()) {
            Optional<Setting<?>> optional = modules.getSetting(module, settingId);

            if (optional.isPresent()) {
                Setting<?> setting = optional.get();
                NbtElement nbtValue = settingsCompound.get(settingId);

                if (nbtValue != null) {
                    setting.getCodec().parse(NbtOps.INSTANCE, nbtValue).resultOrPartial(errorMsg -> logger.error("Failed to decode setting '{}' for module '{}': {}", setting.getId(), setting.getModule().getName(),
                            errorMsg)).ifPresent(setting::castAndSetValue);
                }
            } else {
                logger.warn("Setting '{}' not found in module '{}'", settingId, module.getName());
            }
        }
    }

    /**
     * Represents the configuration data for a single module.
     * This record includes the module's name and a map of its setting values.
     */
    record ModuleConfig(String moduleName, NbtCompound settings) {
        public static final Codec<ModuleConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(Codec.STRING.fieldOf("name").forGetter(ModuleConfig::moduleName), NbtCompound.CODEC.fieldOf("settings").forGetter(ModuleConfig::settings)).apply(instance, ModuleConfig::new));
    }
}
