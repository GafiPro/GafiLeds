package com.gafipro.gafileds.reactive;

import java.util.Arrays;

public final class ColorAnalyzer {
    private static final int BUCKET_COUNT=32*32*32;
    private final int[] counts=new int[BUCKET_COUNT]; private final long[] sumR=new long[BUCKET_COUNT],sumG=new long[BUCKET_COUNT],sumB=new long[BUCKET_COUNT]; private final double[] scores=new double[BUCKET_COUNT];
    public synchronized RgbColor analyze(int[] argb,ReactiveConfig c){Arrays.fill(counts,0);Arrays.fill(sumR,0);Arrays.fill(sumG,0);Arrays.fill(sumB,0);Arrays.fill(scores,0);long rawCount=0,rawR=0,rawG=0,rawB=0;boolean useful=false;int best=-1;double bestScore=-1;
        for(int px:argb){int r=(px>>16)&255,g=(px>>8)&255,b=px&255;rawCount++;rawR+=r;rawG+=g;rawB+=b;int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));double bright=max/255.0,sat=max==0?0:(max-min)/(double)max;if(bright<c.darkThreshold())continue;useful=true;int idx=((r>>3)<<10)|((g>>3)<<5)|(b>>3);counts[idx]++;sumR[idx]+=r;sumG[idx]+=g;sumB[idx]+=b;double score=(1+sat*c.saturationWeight())*(0.35+bright*c.brightnessWeight());scores[idx]+=score;if(scores[idx]>bestScore){bestScore=scores[idx];best=idx;}}
        if(!useful||best<0||counts[best]==0)return rawCount==0?RgbColor.black():new RgbColor((int)(rawR/rawCount),(int)(rawG/rawCount),(int)(rawB/rawCount));long n=counts[best];return new RgbColor((int)Math.round(sumR[best]/(double)n),(int)Math.round(sumG[best]/(double)n),(int)Math.round(sumB[best]/(double)n));
    }
}
