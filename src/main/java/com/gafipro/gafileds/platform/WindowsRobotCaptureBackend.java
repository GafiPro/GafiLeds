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
        try {
            final java.util.concurrent.atomic.AtomicReference<BufferedImage> result = new java.util.concurrent.atomic.AtomicReference<>();
            Runnable captureOnClientThread = () -> {
                NativeImage image = null;
                try {
                    // Use Minecraft's synchronous screenshot path so each capture
                    // reads the framebuffer that exists on the current render frame.
                    // The returned image is immediately converted and closed here.
                    image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
                    result.set(toBufferedImage(image, FULLSCREEN_DOWNSCALE));
                } finally {
                    if (image != null) image.close();
                }
            };

            if (client.isOnThread()) captureOnClientThread.run();
            else client.executeSync(captureOnClientThread);
            return result.get();
        } catch (Throwable t) {
            throw new IllegalStateException("Minecraft framebuffer capture failed", t);
        }
    }

    private static BufferedImage toBufferedImage(NativeImage image, int downscale) {
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int width = Math.max(1, sourceWidth / Math.max(1, downscale));
        int height = Math.max(1, sourceHeight / Math.max(1, downscale));
        int[] pixels = image.copyPixelsArgb();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            int sy = Math.min(sourceHeight - 1, y * downscale);
            for (int x = 0; x < width; x++) {
                int sx = Math.min(sourceWidth - 1, x * downscale);
                output.setRGB(x, y, pixels[sy * sourceWidth + sx]);
            }
        }
        return output;
    }

    @Override
    public java.awt.Dimension outputSize() {
        return new java.awt.Dimension(bounds.width, bounds.height);
    }

    @Override
    public void close() {}
}
