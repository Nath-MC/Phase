package com.purpynaxx.phase.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

public final class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs())
                logger.error("Could not create parent directories for config file: {}", parentDir.getAbsolutePath());
        }

        return configFile;
    }

    /**
     * Saves data to a JSON file using the provided Codec.
     *
     * @param file  The file to save to.
     * @param codec The Codec for the data type T.
     * @param data  The data to save.
     * @param <T>   The type of the data.
     */
    public static <T> void saveData(File file, Codec<T> codec, T data) {
        DataResult<JsonElement> result = codec.encodeStart(JsonOps.INSTANCE, data);
        Optional<JsonElement> jsonElementOptional = Optional.of(result.resultOrPartial(errorMsg -> logger.error("Failed to encode data to JSON for file {}: {}", file.getName(), errorMsg)).orElseThrow());

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(jsonElementOptional.get(), writer);
        } catch (IOException e) {
            logger.error("Failed to write data to config file: {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Loads data from a JSON file using the provided Codec.
     *
     * @param file            The file to load from.
     * @param codec           The Codec for the data type T.
     * @param defaultSupplier A supplier for default data if the file doesn't exist or is corrupt.
     * @param <T>             The type of the data.
     * @return The loaded data, or default data if loading fails or file doesn't exist.
     */
    public static <T> T loadData(File file, Codec<T> codec, Supplier<T> defaultSupplier) {
        if (!file.exists())
            return defaultSupplier.get();

        try (FileReader reader = new FileReader(file)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            DataResult<T> result = codec.parse(JsonOps.INSTANCE, jsonElement);

            Optional<T> loadedDataOptional = result.resultOrPartial(errorMsg -> logger.error("Failed to parse data from config file {}: {}", file.getName(), errorMsg));

            if (loadedDataOptional.isPresent()) {
                return loadedDataOptional.get();
            } else {
                logger.warn("Could not fully decode data from {}, using default data.", file.getName());
                return defaultSupplier.get();
            }

        } catch (IOException e) {
            logger.error("Failed to read config file {}:", file.getAbsolutePath(), e);
            return defaultSupplier.get();
        } catch (Exception e) {
            logger.error("Unexpected error loading data from {}:", file.getAbsolutePath(), e);
            return defaultSupplier.get();
        }
    }
}