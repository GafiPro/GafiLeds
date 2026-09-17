package com.gafipro.gafileds.platform;

import com.gafipro.gafileds.GafiLeds;
import com.gafipro.gafileds.reactive.ReactiveConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Windows capture backend. Normal/windowed Minecraft uses the real Windows
 * desktop via AWT Robot. Minecraft fullscreen uses the rendered Minecraft
 * framebuffer directly because exclusive/accelerated fullscreen can be
 * invisible or stale to desktop capture APIs.
 */
public final class WindowsRobotCaptureBackend implements ScreenCaptureBackend {
    private static final int FULLSCREEN_DOWNSCALE = 4;

    private final MinecraftClient client;
    private final Robot robot;
    private final GraphicsDevice device;
    private final Rectangle bounds;

    public WindowsRobotCaptureBackend(MinecraftClient client, ReactiveConfig config) throws AWTException {
        if (GraphicsEnvironment.isHeadless()) throw new AWTException("Java is running in headless mode");
        this.client = client;
        device = selectDevice(client, config);
        GraphicsConfiguration gc = device.getDefaultConfiguration();
        bounds = gc.getBounds();
        robot = new Robot(device);
        robot.setAutoDelay(0);
        GafiLeds.LOGGER.debug("Using display {} at {}x{}+{},{}", device.getIDstring(), bounds.width, bounds.height, bounds.x, bounds.y);
    }

    private static GraphicsDevice selectDevice(MinecraftClient client, ReactiveConfig c) {
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] ds = env.getScreenDevices();
        if (ds.length == 0) throw new IllegalStateException("No displays found");
        if (c.followMinecraftMonitor()) {
            try {
                var w = client.getWindow();
                int x = w.getX() + Math.max(1, w.getWidth()) / 2;
                int y = w.getY() + Math.max(1, w.getHeight()) / 2;
                for (GraphicsDevice d : ds) if (d.getDefaultConfiguration().getBounds().contains(x, y)) return d;
            } catch (Throwable t) {
                GafiLeds.LOGGER.debug("Could not determine Minecraft monitor: {}", t.getMessage());
            }
        }
        return ds[Math.min(c.monitorIndex(), ds.length - 1)];
    }

    @Override
    public BufferedImage capture() {
        if (client.getWindow().isFullscreen()) {
            BufferedImage minecraft = captureMinecraftFramebuffer();
            if (minecraft != null) return minecraft;
        }
        return robot.createScreenCapture(bounds);
    }

    private BufferedImage captureMinecraftFramebuffer() {
        AtomicReference<BufferedImage> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Runnable captureOnClientThread = () -> {
            try {
                ScreenshotRecorder.takeScreenshot(
                    client.getFramebuffer(),
                    FULLSCREEN_DOWNSCALE,
                    image -> {
                        try {
                            result.set(toBufferedImage(image));
                        } catch (Throwable t) {
                            failure.set(t);
                        } finally {
                            image.close();
                        }
                    }
                );
            } catch (Throwable t) {
                failure.set(t);
            }
        };

        try {
            if (client.isOnThread()) captureOnClientThread.run();
            else client.executeSync(captureOnClientThread);
        } catch (Throwable t) {
            failure.set(t);
        }

        Throwable error = failure.get();
        if (error != null) throw new IllegalStateException("Minecraft framebuffer capture failed", error);
        return result.get();
    }

    private static BufferedImage toBufferedImage(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.copyPixelsArgb();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, width, height, pixels, 0, width);
        return output;
    }

    @Override
    public java.awt.Dimension outputSize() {
        return new java.awt.Dimension(bounds.width, bounds.height);
    }

    @Override
    public void close() {}
}
