package com.gafipro.gafileds.reactive;

import java.awt.image.BufferedImage;

public final class FrameBuffer {
    private final int width, height;
    private final int[] pixels;
    private volatile long capturedAtNanos;
    public FrameBuffer(int width,int height){this.width=width;this.height=height;this.pixels=new int[width*height];}
    public void copyFrom(BufferedImage image){image.getRGB(0,0,width,height,pixels,0,width);capturedAtNanos=System.nanoTime();}
    public int width(){return width;} public int height(){return height;} public int[] pixels(){return pixels;} public long capturedAtNanos(){return capturedAtNanos;}
}
