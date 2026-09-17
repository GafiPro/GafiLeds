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
import java.util.function.Consumer;

/**
 * Windows capture backend. Normal/windowed Minecraft uses the real Windows
 * desktop via AWT Robot. Fullscreen Minecraft is captured from the current
 * Minecraft framebuffer by the render-thread callback path in ScreenCaptureService.
 */
public final class WindowsRobotCaptureBackend implements ScreenCaptureBackend {
    private static final int FULLSCREEN_DOWNSCALE = 4;

    private final MinecraftClient client;
    private final Robot robot;
    private final GraphicsDevice device;
    private final Rectangle bounds;

    public WindowsRobotCaptureBackend(MinecraftClient client, ReactiveConfig config) throws AWTException {
        if (GraphicsEnvironment.isHeadless()) throw new AWTException("Java is running in headless mode");
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
        return robot.createScreenCapture(bounds);
    }

    @Override
    public boolean captureFullscreen(Consumer<BufferedImage> consumer) {
        if (!client.getWindow().isFullscreen() || !client.isOnThread()) return false;

        try {
            ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), FULLSCREEN_DOWNSCALE, image -> {
                try {
                    consumer.accept(toBufferedImage(image));
                } finally {
                    image.close();
                }
            });
            return true;
        } catch (Throwable t) {
            GafiLeds.LOGGER.debug("Minecraft fullscreen framebuffer capture failed: {}", t.getMessage());
            return false;
        }
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
