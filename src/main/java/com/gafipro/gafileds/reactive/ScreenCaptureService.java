package com.gafipro.gafileds.reactive;

import com.gafipro.gafileds.GafiLeds;
import com.gafipro.gafileds.platform.ScreenCaptureBackend;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

public final class ScreenCaptureService implements AutoCloseable {
    private final ScreenCaptureBackend backend; private final FrameStore store; private final AtomicReference<Throwable> lastError=new AtomicReference<>(); private volatile boolean closed;
    public ScreenCaptureService(ScreenCaptureBackend backend,ReactiveConfig config){this.backend=backend;Dimension size=backend.outputSize();store=new FrameStore(3,config.sampleWidth(),config.sampleHeight());GafiLeds.LOGGER.debug("Screen capture output {}x{}, sampled to {}x{}",size.width,size.height,config.sampleWidth(),config.sampleHeight());}
    public FrameStore store(){return store;}
    public boolean captureOnce(){if(closed)return false;FrameBuffer frame=store.acquireForWrite();if(frame==null)return false;BufferedImage image=null;try{image=backend.capture();if(image==null){store.releaseAfterRead(frame);return false;}sample(image,frame);store.publish(frame);return true;}catch(Throwable t){lastError.set(t);GafiLeds.LOGGER.debug("Screen capture failed: {}",t.getMessage());store.releaseAfterRead(frame);return false;}}

    /**
     * Fullscreen Minecraft capture. This method must be called from Minecraft's
     * render thread. The backend owns the GPU screenshot operation and publishes
     * the sampled frame when the screenshot callback supplies the current image.
     */
    public boolean captureFullscreenOnce(){
        if(closed)return false;
        FrameBuffer frame=store.acquireForWrite();
        if(frame==null)return false;
        try{
            boolean started=backend.captureFullscreen(image->{
                try{
                    if(closed){store.releaseAfterRead(frame);return;}
                    sample(image,frame);
                    store.publish(frame);
                }catch(Throwable t){
                    lastError.set(t);
                    GafiLeds.LOGGER.debug("Fullscreen frame processing failed: {}",t.getMessage());
                    store.releaseAfterRead(frame);
                }
            });
            if(!started)store.releaseAfterRead(frame);
            return started;
        }catch(Throwable t){
            lastError.set(t);
            GafiLeds.LOGGER.debug("Fullscreen capture request failed: {}",t.getMessage());
            store.releaseAfterRead(frame);
            return false;
        }
    }

    private void sample(BufferedImage image,FrameBuffer frame){int w=image.getWidth(),h=image.getHeight(),tw=frame.width(),th=frame.height();int[] p=frame.pixels();for(int y=0;y<th;y++){int sy=th==1?0:Math.min(h-1,(int)((long)y*h/th));for(int x=0;x<tw;x++){int sx=tw==1?0:Math.min(w-1,(int)((long)x*w/tw));p[y*tw+x]=image.getRGB(sx,sy);}}}
    public Throwable lastError(){return lastError.get();}
    public void close(){closed=true;backend.close();}
}
