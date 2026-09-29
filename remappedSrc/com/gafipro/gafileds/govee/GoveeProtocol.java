package com.gafipro.gafileds.govee;

import com.gafipro.gafileds.reactive.RgbColor;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;

public final class GoveeProtocol {
    public static final String MULTICAST_ADDRESS="239.255.255.250"; private GoveeProtocol(){}
    public static byte[] scanPacket(){JsonObject root=new JsonObject(),msg=new JsonObject(),data=new JsonObject();msg.addProperty("cmd","scan");data.addProperty("account_topic","reserve");msg.add("data",data);root.add("msg",msg);return root.toString().getBytes(StandardCharsets.UTF_8);}
    public static byte[] turn(boolean on){return simpleValueCommand("turn",on?1:0);}
    public static byte[] brightness(int value){return simpleValueCommand("brightness",Math.max(1,Math.min(100,value)));}
    public static byte[] color(RgbColor c){JsonObject root=new JsonObject(),msg=new JsonObject(),data=new JsonObject(),rgb=new JsonObject();msg.addProperty("cmd","colorwc");rgb.addProperty("r",c.r());rgb.addProperty("g",c.g());rgb.addProperty("b",c.b());data.add("color",rgb);data.addProperty("colorTemInKelvin",0);msg.add("data",data);root.add("msg",msg);return root.toString().getBytes(StandardCharsets.UTF_8);}
    public static byte[] status(){JsonObject root=new JsonObject(),msg=new JsonObject();msg.addProperty("cmd","devStatus");msg.add("data",new JsonObject());root.add("msg",msg);return root.toString().getBytes(StandardCharsets.UTF_8);}
    private static byte[] simpleValueCommand(String cmd,int value){JsonObject root=new JsonObject(),msg=new JsonObject(),data=new JsonObject();msg.addProperty("cmd",cmd);data.addProperty("value",value);msg.add("data",data);root.add("msg",msg);return root.toString().getBytes(StandardCharsets.UTF_8);}
}
