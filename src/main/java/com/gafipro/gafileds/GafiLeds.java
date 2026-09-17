package com.gafipro.gafileds;

import com.gafipro.gafileds.command.GoveeCommands;
import com.gafipro.gafileds.config.ConfigManager;
import com.gafipro.gafileds.config.GafiLedsConfig;
import com.gafipro.gafileds.govee.GoveeClient;
import com.gafipro.gafileds.govee.GoveeCommandScheduler;
import com.gafipro.gafileds.govee.GoveeDiscovery;
import com.gafipro.gafileds.reactive.ReactiveController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GafiLeds implements ClientModInitializer {
    public static final String MOD_ID = "gafileds";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static GafiLeds INSTANCE;
    private ConfigManager configManager;
    private GoveeDiscovery discovery;
    private GoveeClient goveeClient;
    private GoveeCommandScheduler commandScheduler;
    private ReactiveController reactiveController;

    public static GafiLeds getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("GafiLeds is not initialized");
        return INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        configManager = new ConfigManager(MinecraftClient.getInstance().runDirectory.toPath());
        GafiLedsConfig config = configManager.load();
        discovery = new GoveeDiscovery(config.network().discoveryTimeoutMs(), config.network().discoveryPort());
        goveeClient = new GoveeClient(config.network().controlTimeoutMs());
        commandScheduler = new GoveeCommandScheduler(goveeClient, config);
        reactiveController = new ReactiveController(configManager, config, commandScheduler);
        GoveeCommands.register(this);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> reactiveController.onRenderFrame());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
        LOGGER.info("GafiLeds {} initialized", configManager.version());
    }

    public ConfigManager configManager() { return configManager; }
    public GoveeDiscovery discovery() { return discovery; }
    public GoveeClient goveeClient() { return goveeClient; }
    public GoveeCommandScheduler commandScheduler() { return commandScheduler; }
    public ReactiveController reactiveController() { return reactiveController; }

    public synchronized void reloadConfig() {
        GafiLedsConfig config = configManager.load();
        commandScheduler.applyConfig(config);
        reactiveController.applyConfig(config);
        LOGGER.info("Configuration reloaded");
    }

    public synchronized void shutdown() {
        if (reactiveController != null) reactiveController.close();
        if (commandScheduler != null) commandScheduler.close();
        if (goveeClient != null) goveeClient.close();
        if (discovery != null) discovery.close();
    }
}
