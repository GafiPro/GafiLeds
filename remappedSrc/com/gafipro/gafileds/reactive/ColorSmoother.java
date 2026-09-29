package com.gafipro.gafileds.reactive;

public final class ColorSmoother {
    private RgbColor current,target;
    public synchronized RgbColor updateTarget(RgbColor observed,double threshold){if(current==null){current=observed;target=observed;return current;}if(current.distanceNormalized(observed)>=threshold)target=observed;return target;}
    public synchronized RgbColor step(double dt,double responsiveness){if(current==null)return null;if(target==null)return current;double lambda=2+18*Math.max(0.01,Math.min(1,responsiveness));double a=1-Math.exp(-lambda*Math.max(0,dt));current=current.lerp(target,a);return current;}
    public synchronized RgbColor current(){return current;} public synchronized RgbColor target(){return target;} public synchronized void reset(RgbColor color){current=color;target=color;}
}
