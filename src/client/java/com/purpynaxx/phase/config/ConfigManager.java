package com.purpynaxx.phase.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.purpynaxx.phase.Phase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * ConfigManager is responsible for managing configuration files.
 * It provides methods to save and load data in NBT format, ensuring the correct directory structure.
 */
public final class ConfigManager {

    static final Logger logger = LoggerFactory.getLogger("Phase/ConfigManager");
    private static final String root = "";

    private ConfigManager() {}

    /**
     * Resolves a config file path within the default mod config directory.
     * Creates the directory and any necessary parent directories if they don't exist.
     *
     * @param path The path of the file
     * @return The File object representing the file.
     */
    public static File getConfigFile(String path) {
        File modBaseDir = new File(MinecraftClient.getInstance().runDirectory, "phase");
        File configFile = new File(modBaseDir, path);
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
     * @param file  The file to save to.
     * @param codec The Codec for the data type T.
     * @param data  The data to save.
     * @param <T>   The type of the data.
     */
    public static <T> void saveData(File file, Codec<T> codec, T data) {

        File nbtFile = ensureNbtFile(file);

        DataResult<NbtElement> result = codec.encodeStart(NbtOps.INSTANCE, data);
        Optional<NbtElement> nbtElementOptional = Optional.of(result.resultOrPartial(errorMsg -> logger.error("Failed to encode data to NBT for file {}: {}", nbtFile.getName(), errorMsg)).orElseThrow());

        NbtElement encodedElement = nbtElementOptional.get();
        NbtCompound rootCompound = new NbtCompound();
        rootCompound.put(root, encodedElement);

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
     * @param file            The file to load from.
     * @param codec           The Codec for the data type T.
     * @param defaultSupplier A supplier for default data if the file doesn't exist or is corrupt.
     * @param <T>             The type of the data.
     * @return The loaded data, or default data if loading fails or file doesn't exist.
     */
    public static <T> T loadData(File file, Codec<T> codec, Supplier<T> defaultSupplier) {

        File nbtFile = ensureNbtFile(file);

        if (!nbtFile.exists()) {
            if (Phase.IS_DEV_ENVIRONMENT) {
                logger.warn("The specified file \"{}\" has not been found, using default data.", file.getName());
            }
            return defaultSupplier.get();
        }

        try {
            NbtCompound rootCompound = NbtIo.read((nbtFile.toPath()));

            if (rootCompound != null && rootCompound.contains(root)) {
                NbtElement encodedElement = rootCompound.get(root);
                DataResult<T> result = codec.parse(NbtOps.INSTANCE, encodedElement);
                Optional<T> loadedDataOptional = Optional.of(result.resultOrPartial(errorMsg -> logger.error("Failed to parse data from config file {}: {}", nbtFile.getName(), errorMsg)).orElseThrow());
                return loadedDataOptional.get();
            } else {
                logger.warn("Config file {} does not contain the expected root key '{}', using default data.", nbtFile.getName(), root);
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

}
