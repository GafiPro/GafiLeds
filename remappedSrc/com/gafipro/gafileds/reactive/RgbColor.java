package com.gafipro.gafileds.reactive;

import java.util.Locale;

public record RgbColor(int r, int g, int b) {
    public RgbColor { r = clamp(r); g = clamp(g); b = clamp(b); }
    public static RgbColor black() { return new RgbColor(0, 0, 0); }
    public static RgbColor parseHex(String value) {
        if (value == null) throw new IllegalArgumentException("Color cannot be null");
        String hex = value.trim(); if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("(?i)[0-9a-f]{6}")) throw new IllegalArgumentException("Expected RGB hex color such as #FF8800");
        int packed = Integer.parseInt(hex, 16);
        return new RgbColor((packed >> 16) & 255, (packed >> 8) & 255, packed & 255);
    }
    public String toHex() { return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b); }
    public double distanceNormalized(RgbColor other) { int dr=r-other.r(), dg=g-other.g(), db=b-other.b(); return Math.sqrt((double)dr*dr+(double)dg*dg+(double)db*db)/441.67295593; }
    public RgbColor lerp(RgbColor target, double alpha) { double a=Math.max(0,Math.min(1,alpha)); return new RgbColor((int)Math.round(r+(target.r-r)*a),(int)Math.round(g+(target.g-g)*a),(int)Math.round(b+(target.b-b)*a)); }
    public double brightness() { return (0.2126*r+0.7152*g+0.0722*b)/255.0; }
    public double saturation() { double max=Math.max(r,Math.max(g,b)); if(max<=0)return 0; double min=Math.min(r,Math.min(g,b)); return (max-min)/max; }
    public int packedRgb() { return (r<<16)|(g<<8)|b; }
    private static int clamp(int c) { return Math.max(0, Math.min(255,c)); }
}
