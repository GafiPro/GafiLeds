package com.gafipro.gafileds.reactive;

import java.util.concurrent.atomic.AtomicIntegerArray;

public final class FrameStore {
    private static final int FREE=0,WRITING=1,READY=2,READING=3;
    private final FrameBuffer[] buffers; private final AtomicIntegerArray states;
    public FrameStore(int count,int width,int height){if(count<2)throw new IllegalArgumentException("At least two frame buffers are required");buffers=new FrameBuffer[count];states=new AtomicIntegerArray(count);for(int i=0;i<count;i++)buffers[i]=new FrameBuffer(width,height);}
    public FrameBuffer acquireForWrite(){for(int i=0;i<buffers.length;i++)if(states.compareAndSet(i,FREE,WRITING))return buffers[i];return null;}
    public void publish(FrameBuffer b){states.set(indexOf(b),READY);}
    public FrameBuffer acquireNewestForRead(){int newest=-1;long ts=Long.MIN_VALUE;for(int i=0;i<buffers.length;i++)if(states.get(i)==READY&&buffers[i].capturedAtNanos()>ts){newest=i;ts=buffers[i].capturedAtNanos();}if(newest<0||!states.compareAndSet(newest,READY,READING))return null;for(int i=0;i<buffers.length;i++)if(i!=newest)states.compareAndSet(i,READY,FREE);return buffers[newest];}
    public void releaseAfterRead(FrameBuffer b){states.set(indexOf(b),FREE);}
    private int indexOf(FrameBuffer b){for(int i=0;i<buffers.length;i++)if(buffers[i]==b)return i;throw new IllegalArgumentException("Unknown frame buffer");}
}
