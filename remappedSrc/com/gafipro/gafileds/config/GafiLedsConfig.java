package com.gafipro.gafileds.config;

import com.gafipro.gafileds.reactive.ReactiveConfig;
import java.util.Objects;

public record GafiLedsConfig(String selectedDeviceId, String selectedDeviceIp, String selectedDeviceModel, int brightness, NetworkConfig network, ReactiveConfig reactive) {
    public GafiLedsConfig {
        selectedDeviceId = selectedDeviceId == null ? "" : selectedDeviceId;
        selectedDeviceIp = selectedDeviceIp == null ? "" : selectedDeviceIp;
        selectedDeviceModel = selectedDeviceModel == null ? "" : selectedDeviceModel;
        brightness = Math.max(1, Math.min(100, brightness));
        network = Objects.requireNonNull(network);
        reactive = Objects.requireNonNull(reactive);
    }
    public static GafiLedsConfig defaults() { return new GafiLedsConfig("", "", "", 80, NetworkConfig.defaults(), ReactiveConfig.defaults()); }
    public GafiLedsConfig withSelectedDevice(String id, String ip, String model) { return new GafiLedsConfig(id, ip, model, brightness, network, reactive); }
    public GafiLedsConfig withBrightness(int value) { return new GafiLedsConfig(selectedDeviceId, selectedDeviceIp, selectedDeviceModel, value, network, reactive); }
    public GafiLedsConfig withReactive(ReactiveConfig value) { return new GafiLedsConfig(selectedDeviceId, selectedDeviceIp, selectedDeviceModel, brightness, network, value); }

    public record NetworkConfig(int discoveryTimeoutMs, int discoveryPort, int controlPort, int controlTimeoutMs, boolean autoDiscovery, boolean debug) {
        public NetworkConfig {
            discoveryTimeoutMs = Math.max(250, Math.min(10_000, discoveryTimeoutMs));
            discoveryPort = discoveryPort <= 0 ? 4002 : discoveryPort;
            controlPort = controlPort <= 0 ? 4003 : controlPort;
            controlTimeoutMs = Math.max(50, Math.min(2_000, controlTimeoutMs));
        }
        public static NetworkConfig defaults() { return new NetworkConfig(2000, 4002, 4003, 500, true, false); }
    }
}
