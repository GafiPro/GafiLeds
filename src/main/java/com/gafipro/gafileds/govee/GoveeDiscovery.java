package com.gafipro.gafileds.govee;

import com.gafipro.gafileds.GafiLeds;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GoveeDiscovery implements Closeable {
    private final int timeoutMs,listenPort;private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"GafiLeds-GoveeDiscovery");t.setDaemon(true);return t;});private volatile List<GoveeDevice> lastResults=List.of();
    public GoveeDiscovery(int timeoutMs,int listenPort){this.timeoutMs=timeoutMs;this.listenPort=listenPort;}
    public CompletableFuture<List<GoveeDevice>> scanAsync(){return CompletableFuture.supplyAsync(this::scanBlocking,executor);}
    public List<GoveeDevice> lastResults(){return lastResults;}
    public List<GoveeDevice> scanBlocking(){Map<String,GoveeDevice> devices=new LinkedHashMap<>();long end=System.nanoTime()+Duration.ofMillis(timeoutMs).toNanos();try(MulticastSocket socket=new MulticastSocket(null)){socket.setReuseAddress(true);socket.bind(new InetSocketAddress(listenPort));socket.setSoTimeout(200);byte[] payload=GoveeProtocol.scanPacket();socket.send(new DatagramPacket(payload,payload.length,InetAddress.getByName(GoveeProtocol.MULTICAST_ADDRESS),4001));byte[] buffer=new byte[4096];while(System.nanoTime()<end){try{DatagramPacket p=new DatagramPacket(buffer,buffer.length);socket.receive(p);GoveeDevice d=parse(p.getAddress(),p.getData(),p.getLength());if(d!=null)devices.put(d.id(),d);}catch(java.net.SocketTimeoutException ignored){}}}catch(Exception e){GafiLeds.LOGGER.warn("Govee LAN discovery failed: {}",e.getMessage());}lastResults=List.copyOf(devices.values());return lastResults;}
    private GoveeDevice parse(InetAddress source,byte[] bytes,int length){try{JsonElement el=JsonParser.parseString(new String(bytes,0,length,StandardCharsets.UTF_8));if(!el.isJsonObject())return null;JsonObject root=el.getAsJsonObject(),msg=root.getAsJsonObject("msg");if(msg==null||!"scan".equals(msg.get("cmd").getAsString()))return null;JsonObject data=msg.getAsJsonObject("data");if(data==null)return null;String id=string(data,"device",source.getHostAddress()),model=string(data,"sku","unknown"),name=string(data,"deviceName",model),ip=string(data,"ip",source.getHostAddress());return new GoveeDevice(id,model,name,InetAddress.getByName(ip));}catch(Exception ignored){return null;}}
    private static String string(JsonObject o,String key,String fallback){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():fallback;}
    public void close(){executor.shutdownNow();}
}
