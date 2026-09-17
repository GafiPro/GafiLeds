package com.gafipro.gafileds.govee;

import com.gafipro.gafileds.GafiLeds;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GoveeDiscovery implements Closeable {
    private static final int RESPONSE_PORT = 4002;
    private static final int SCAN_PORT = 4001;
    private final int timeoutMs;
    private final int listenPort;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GafiLeds-GoveeDiscovery");
        t.setDaemon(true);
        return t;
    });
    private volatile List<GoveeDevice> lastResults = List.of();

    public GoveeDiscovery(int timeoutMs, int listenPort) {
        this.timeoutMs = timeoutMs;
        // Govee's LAN API uses a fixed response port. Keep the configured value for
        // compatibility, but never allow discovery to silently listen somewhere else.
        this.listenPort = RESPONSE_PORT;
    }

    public CompletableFuture<List<GoveeDevice>> scanAsync() {
        return CompletableFuture.supplyAsync(this::scanBlocking, executor);
    }

    public List<GoveeDevice> lastResults() {
        return lastResults;
    }

    public List<GoveeDevice> scanBlocking() {
        Map<String, GoveeDevice> devices = new LinkedHashMap<>();
        long end = System.nanoTime() + Duration.ofMillis(timeoutMs).toNanos();
        byte[] payload = GoveeProtocol.scanPacket();
        int packetsSent = 0;
        int interfacesUsed = 0;

        try (MulticastSocket socket = new MulticastSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(listenPort));
            socket.setSoTimeout(Math.min(200, Math.max(50, timeoutMs / 4)));

            List<NetworkInterface> interfaces = activeIpv4Interfaces();
            for (NetworkInterface nif : interfaces) {
                try {
                    socket.setNetworkInterface(nif);
                    socket.joinGroup(new InetSocketAddress(InetAddress.getByName(GoveeProtocol.MULTICAST_ADDRESS), SCAN_PORT), nif);
                    interfacesUsed++;

                    DatagramPacket multicast = new DatagramPacket(
                            payload,
                            payload.length,
                            InetAddress.getByName(GoveeProtocol.MULTICAST_ADDRESS),
                            SCAN_PORT
                    );
                    socket.send(multicast);
                    packetsSent++;

                    // A second packet helps on Wi-Fi APs that occasionally drop the first
                    // multicast frame while their IGMP state is being refreshed.
                    socket.send(multicast);
                    packetsSent++;

                    // Some routers/APs block multicast UDP while allowing subnet broadcast.
                    for (InterfaceAddress ia : nif.getInterfaceAddresses()) {
                        if (!(ia.getAddress() instanceof Inet4Address) || ia.getBroadcast() == null) continue;
                        DatagramPacket broadcast = new DatagramPacket(
                                payload,
                                payload.length,
                                ia.getBroadcast(),
                                SCAN_PORT
                        );
                        try {
                            socket.send(broadcast);
                            packetsSent++;
                        } catch (Exception ignored) {
                            // Broadcast is only a fallback; multicast is still the primary path.
                        }
                    }
                } catch (Exception e) {
                    GafiLeds.LOGGER.debug("Govee discovery interface {} failed: {}", nif.getName(), e.getMessage());
                }
            }

            // Final compatibility attempt using the JVM's default multicast route.
            try {
                socket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
            } catch (Exception ignored) {
                // The per-interface sends above already covered the normal case.
            }
            try {
                DatagramPacket multicast = new DatagramPacket(
                        payload,
                        payload.length,
                        InetAddress.getByName(GoveeProtocol.MULTICAST_ADDRESS),
                        SCAN_PORT
                );
                socket.send(multicast);
                packetsSent++;
            } catch (Exception ignored) {
                // No-op; results are collected below.
            }

            if (GafiLeds.LOGGER.isDebugEnabled()) {
                GafiLeds.LOGGER.debug("Govee discovery sent {} packet(s) across {} interface(s), listening on UDP {}", packetsSent, interfacesUsed, listenPort);
            }

            byte[] buffer = new byte[4096];
            while (System.nanoTime() < end) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    GoveeDevice device = parse(packet.getAddress(), packet.getData(), packet.getLength());
                    if (device != null) {
                        devices.put(device.id(), device);
                        GafiLeds.LOGGER.debug("Govee discovery response from {} -> {} ({})", packet.getAddress().getHostAddress(), device.model(), device.id());
                    }
                } catch (java.net.SocketTimeoutException ignored) {
                    // Continue until the overall scan deadline.
                }
            }
        } catch (Exception e) {
            GafiLeds.LOGGER.warn("Govee LAN discovery failed: {}", e.getMessage());
        }

        lastResults = List.copyOf(devices.values());
        return lastResults;
    }

    private static List<NetworkInterface> activeIpv4Interfaces() {
        List<NetworkInterface> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
            while (all != null && all.hasMoreElements()) {
                NetworkInterface nif = all.nextElement();
                try {
                    if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                    boolean hasIpv4 = false;
                    Enumeration<InetAddress> addresses = nif.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        if (addresses.nextElement() instanceof Inet4Address) {
                            hasIpv4 = true;
                            break;
                        }
                    }
                    if (hasIpv4) result.add(nif);
                } catch (Exception ignored) {
                    // Ignore interfaces that disappear or cannot be queried while scanning.
                }
            }
        } catch (Exception ignored) {
            // Return an empty list and let the default-route compatibility attempt run.
        }
        return result;
    }

    private GoveeDevice parse(InetAddress source, byte[] bytes, int length) {
        try {
            JsonElement element = JsonParser.parseString(new String(bytes, 0, length, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) return null;
            JsonObject root = element.getAsJsonObject();
            JsonObject msg = root.getAsJsonObject("msg");
            if (msg == null || !msg.has("cmd") || !"scan".equals(msg.get("cmd").getAsString())) return null;
            JsonObject data = msg.getAsJsonObject("data");
            if (data == null) return null;
            String id = string(data, "device", source.getHostAddress());
            String model = string(data, "sku", "unknown");
            String name = string(data, "deviceName", model);
            // Trust the UDP source address for actual routing rather than a payload-provided IP.
            return new GoveeDevice(id, model, name, source);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }

    public void close() {
        executor.shutdownNow();
    }
}
