package com.gafipro.gafileds.platform;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public interface ScreenCaptureBackend extends AutoCloseable { BufferedImage capture() throws Exception; Dimension outputSize(); @Override void close(); }
