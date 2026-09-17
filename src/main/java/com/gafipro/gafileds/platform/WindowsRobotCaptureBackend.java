package com.gafipro.gafileds.platform;

import com.gafipro.gafileds.GafiLeds;
import com.gafipro.gafileds.reactive.ReactiveConfig;
import net.minecraft.client.MinecraftClient;
import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

public final class WindowsRobotCaptureBackend implements ScreenCaptureBackend {
    private final Robot robot; private final GraphicsDevice device; private final Rectangle bounds;
    public WindowsRobotCaptureBackend(MinecraftClient client,ReactiveConfig config)throws AWTException{if(GraphicsEnvironment.isHeadless())throw new AWTException("Java is running in headless mode");device=selectDevice(client,config);GraphicsConfiguration gc=device.getDefaultConfiguration();bounds=gc.getBounds();robot=new Robot(device);robot.setAutoDelay(0);GafiLeds.LOGGER.debug("Using display {} at {}x{}+{},{}",device.getIDstring(),bounds.width,bounds.height,bounds.x,bounds.y);}
    private static GraphicsDevice selectDevice(MinecraftClient client,ReactiveConfig c){GraphicsEnvironment env=GraphicsEnvironment.getLocalGraphicsEnvironment();GraphicsDevice[] ds=env.getScreenDevices();if(ds.length==0)throw new IllegalStateException("No displays found");if(c.followMinecraftMonitor())try{net.minecraft.client.util.Window w=client.getWindow();int x=w.getX()+Math.max(1,w.getWidth())/2,y=w.getY()+Math.max(1,w.getHeight())/2;for(GraphicsDevice d:ds)if(d.getDefaultConfiguration().getBounds().contains(x,y))return d;}catch(Throwable t){GafiLeds.LOGGER.debug("Could not determine Minecraft monitor: {}",t.getMessage());}return ds[Math.min(c.monitorIndex(),ds.length-1)];}
    public BufferedImage capture(){return robot.createScreenCapture(bounds);} public java.awt.Dimension outputSize(){return new java.awt.Dimension(bounds.width,bounds.height);} public void close(){}
}
