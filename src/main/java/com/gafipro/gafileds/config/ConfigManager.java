package com.gafipro.gafileds.config;

import com.gafipro.gafileds.GafiLeds;
import com.gafipro.gafileds.reactive.ReactiveConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final String FILE_NAME = "gafileds.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String VERSION = "1.0.0";
    private final Path file;
    private volatile GafiLedsConfig config = GafiLedsConfig.defaults();

    public ConfigManager(Path gameDirectory) { file = gameDirectory.resolve("config").resolve(FILE_NAME); }
    public String version() { return VERSION; }
    public GafiLedsConfig get() { return config; }

    public synchronized GafiLedsConfig load() {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) { config = GafiLedsConfig.defaults(); save(); return config; }
            GafiLedsConfig loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), GafiLedsConfig.class);
            config = normalize(loaded);
            if (loaded == null) save();
        } catch (Exception e) {
            GafiLeds.LOGGER.error("Failed to load configuration from {}. Using defaults.", file, e);
            config = GafiLedsConfig.defaults();
        }
        return config;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(config), StandardCharsets.UTF_8);
            try { Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { GafiLeds.LOGGER.error("Failed to save configuration to {}", file, e); }
    }

    public synchronized void update(GafiLedsConfig newConfig) { config = normalize(newConfig); save(); }

    private static GafiLedsConfig normalize(GafiLedsConfig value) {
        if (value == null) return GafiLedsConfig.defaults();
        GafiLedsConfig.NetworkConfig network = value.network() == null ? GafiLedsConfig.NetworkConfig.defaults() : value.network();
        ReactiveConfig reactive = value.reactive() == null ? ReactiveConfig.defaults() : value.reactive();
        return new GafiLedsConfig(value.selectedDeviceId(), value.selectedDeviceIp(), value.selectedDeviceModel(), value.brightness(), network, reactive);
    }
}
