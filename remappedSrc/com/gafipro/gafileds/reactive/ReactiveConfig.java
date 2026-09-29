package com.gafipro.gafileds.reactive;

public record ReactiveConfig(boolean enabled, int captureFps, int analysisFps, int updateFps, int sampleWidth, int sampleHeight, double smoothing, double threshold, double darkThreshold, double saturationWeight, double brightnessWeight, boolean followMinecraftMonitor, int monitorIndex, boolean darkScenesReduceBrightness) {
    public ReactiveConfig {
        captureFps = clamp(captureFps, 5, 60); analysisFps = clamp(analysisFps, 5, 60); updateFps = clamp(updateFps, 1, 30);
        sampleWidth = clamp(sampleWidth, 16, 128); sampleHeight = clamp(sampleHeight, 9, 72); smoothing = clamp(smoothing, 0.01, 1.0);
        threshold = clamp(threshold, 0.0, 1.0); darkThreshold = clamp(darkThreshold, 0.0, 1.0); saturationWeight = clamp(saturationWeight, 0.0, 4.0); brightnessWeight = clamp(brightnessWeight, 0.0, 4.0); monitorIndex = Math.max(0, monitorIndex);
    }
    public static ReactiveConfig defaults() { return new ReactiveConfig(false, 30, 25, 20, 48, 27, 0.15, 0.035, 0.055, 0.9, 0.35, true, 0, false); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static double clamp(double v, double min, double max) { return Double.isFinite(v) ? Math.max(min, Math.min(max, v)) : min; }
}
