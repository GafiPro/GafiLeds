package com.gafipro.gafileds.govee;

import com.gafipro.gafileds.reactive.RgbColor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class GoveeClient implements Closeable {
    private final int timeoutMs; private final DatagramSocket socket;
    public GoveeClient(int timeoutMs){this.timeoutMs=timeoutMs;try{socket=new DatagramSocket();socket.setSoTimeout(timeoutMs);}catch(Exception e){throw new IllegalStateException("Unable to create Govee UDP socket",e);}}
    public void send(InetAddress address,byte[] payload)throws java.io.IOException{socket.send(new DatagramPacket(payload,payload.length,address,4003));}
    public void sendColor(InetAddress address,RgbColor c)throws java.io.IOException{send(address,GoveeProtocol.color(c));}public void sendOn(InetAddress address,boolean on)throws java.io.IOException{send(address,GoveeProtocol.turn(on));}public void sendBrightness(InetAddress address,int b)throws java.io.IOException{send(address,GoveeProtocol.brightness(b));}
    public Optional<GoveeStatus> requestStatus(InetAddress address){try(DatagramSocket s=new DatagramSocket(4002)){s.setSoTimeout(timeoutMs);s.send(new DatagramPacket(GoveeProtocol.status(),GoveeProtocol.status().length,address,4003));byte[] b=new byte[4096];long end=System.nanoTime()+timeoutMs*1_000_000L;while(System.nanoTime()<end){try{DatagramPacket p=new DatagramPacket(b,b.length);s.receive(p);if(address.equals(p.getAddress())){GoveeStatus status=parseStatus(p.getData(),p.getLength());if(status!=null)return Optional.of(status);}}catch(java.net.SocketTimeoutException e){break;}}}catch(Exception ignored){}return Optional.empty();}
    private GoveeStatus parseStatus(byte[] b,int length){try{JsonElement root=JsonParser.parseString(new String(b,0,length,StandardCharsets.UTF_8));if(!root.isJsonObject())return null;JsonObject msg=root.getAsJsonObject().getAsJsonObject("msg");if(msg==null||!"devStatus".equals(msg.get("cmd").getAsString()))return null;JsonObject d=msg.getAsJsonObject("data");if(d==null)return null;Integer on=d.has("onOff")?d.get("onOff").getAsInt():null,br=d.has("brightness")?d.get("brightness").getAsInt():null;RgbColor c=null;if(d.has("color")&&d.get("color").isJsonObject()){JsonObject x=d.getAsJsonObject("color");if(x.has("r")&&x.has("g")&&x.has("b"))c=new RgbColor(x.get("r").getAsInt(),x.get("g").getAsInt(),x.get("b").getAsInt());}return new GoveeStatus(on,br,c);}catch(Exception ignored){return null;}}
    public void close(){socket.close();}
}
