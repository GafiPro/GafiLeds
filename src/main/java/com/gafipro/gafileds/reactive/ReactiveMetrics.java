package com.gafipro.gafileds.reactive;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class ReactiveMetrics {
    private final AtomicLong captured=new AtomicLong(),analyzed=new AtomicLong(),sent=new AtomicLong(),dropped=new AtomicLong(),errors=new AtomicLong();
    private final AtomicReference<RgbColor> observed=new AtomicReference<>(),target=new AtomicReference<>(),current=new AtomicReference<>();
    private volatile long last=System.nanoTime(),sampleC,sampleA,sampleS; private volatile double captureFps,analysisFps,updateFps;
    public void captured(){captured.incrementAndGet();refresh();} public void analyzed(){analyzed.incrementAndGet();refresh();} public void sent(){sent.incrementAndGet();refresh();} public void droppedFrame(){dropped.incrementAndGet();} public void error(){errors.incrementAndGet();}
    public void observed(RgbColor c){observed.set(c);} public void target(RgbColor c){target.set(c);} public void current(RgbColor c){current.set(c);}
    private synchronized void refresh(){long now=System.nanoTime(),elapsed=now-last;if(elapsed<1_000_000_000L)return;double s=elapsed/1e9;long c=captured.get(),a=analyzed.get(),u=sent.get();captureFps=(c-sampleC)/s;analysisFps=(a-sampleA)/s;updateFps=(u-sampleS)/s;sampleC=c;sampleA=a;sampleS=u;last=now;}
    public long capturedCount(){return captured.get();} public long analyzedCount(){return analyzed.get();} public long sentCount(){return sent.get();} public long droppedFrames(){return dropped.get();} public long errors(){return errors.get();}
    public double captureFps(){return captureFps;} public double analysisFps(){return analysisFps;} public double updateFps(){return updateFps;} public RgbColor observed(){return observed.get();} public RgbColor target(){return target.get();} public RgbColor current(){return current.get();}
}
