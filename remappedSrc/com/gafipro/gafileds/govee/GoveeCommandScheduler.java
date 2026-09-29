package com.gafipro.gafileds.govee;

import com.gafipro.gafileds.GafiLeds;
import com.gafipro.gafileds.config.GafiLedsConfig;
import com.gafipro.gafileds.reactive.RgbColor;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

public final class GoveeCommandScheduler implements AutoCloseable {
    private record Pending(InetAddress address,RgbColor color,boolean force){}
    private final GoveeClient client;private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"GafiLeds-GoveeUpdate");t.setDaemon(true);return t;});private final AtomicReference<Pending> latest=new AtomicReference<>();private final AtomicBoolean scheduled=new AtomicBoolean();private final AtomicLong lastSentNanos=new AtomicLong();private volatile long minIntervalNanos=50_000_000L;private volatile boolean closed;private volatile RgbColor lastSent;
    public GoveeCommandScheduler(GoveeClient client,GafiLedsConfig config){this.client=client;applyConfig(config);}public void applyConfig(GafiLedsConfig config){minIntervalNanos=1_000_000_000L/Math.max(1,config.reactive().updateFps());}
    public void submitColor(InetAddress a,RgbColor c){submitColor(a,c,false);}public void submitColor(InetAddress a,RgbColor c,boolean force){if(closed)return;if(!force&&c.equals(lastSent))return;latest.set(new Pending(a,c,force));schedule(0);}
    public void sendOn(InetAddress a,boolean on){immediate(()->client.sendOn(a,on));}public void sendBrightness(InetAddress a,int b){immediate(()->client.sendBrightness(a,b));}public void sendColorImmediate(InetAddress a,RgbColor c){immediate(()->{client.sendColor(a,c);lastSent=c;});}
    private void immediate(Io op){if(closed)return;executor.execute(()->{try{op.run();}catch(Exception e){GafiLeds.LOGGER.debug("Govee command failed: {}",e.getMessage());}});}
    private void schedule(long delay){if(closed||scheduled.getAndSet(true))return;long now=System.nanoTime(),wait=Math.max(delay,Math.max(0,lastSentNanos.get()+minIntervalNanos-now));executor.schedule(this::drain,wait,TimeUnit.NANOSECONDS);}
    private void drain(){try{if(closed)return;Pending p=latest.getAndSet(null);if(p==null)return;try{client.sendColor(p.address(),p.color());lastSent=p.color();lastSentNanos.set(System.nanoTime());}catch(Exception e){GafiLeds.LOGGER.debug("Reactive Govee update failed: {}",e.getMessage());}}finally{scheduled.set(false);if(!closed&&latest.get()!=null)schedule(0);}}
    public RgbColor lastSent(){return lastSent;}public void close(){closed=true;latest.set(null);executor.shutdownNow();}
    @FunctionalInterface private interface Io{void run()throws Exception;}
}
