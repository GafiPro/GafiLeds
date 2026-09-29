package com.gafipro.gafileds.platform;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public interface ScreenCaptureBackend extends AutoCloseable {
    BufferedImage capture() throws Exception;
    Dimension outputSize();

    /**
     * Requests a fullscreen frame capture from the render thread.
     * Implementations may invoke the consumer synchronously or asynchronously,
     * but must invoke it with the newly captured frame when the request succeeds.
     */
    default boolean captureFullscreen(Consumer<BufferedImage> consumer) throws Exception {
        return false;
    }

    @Override
    void close();
}
