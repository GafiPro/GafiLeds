package com.gafipro.gafileds.govee;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Objects;

public final class GoveeDevice {
    private final String id,model,name;private final InetAddress address;private final GoveeCapabilities capabilities;private volatile Instant lastSeen;private volatile boolean online=true;private volatile Integer brightness,onOff;
    public GoveeDevice(String id,String model,String name,InetAddress address){this.id=Objects.requireNonNullElse(id,"");this.model=Objects.requireNonNullElse(model,"unknown");this.name=Objects.requireNonNullElse(name,model);this.address=Objects.requireNonNull(address);capabilities=GoveeCapabilities.fromModel(model);lastSeen=Instant.now();}
    public String id(){return id;}public String model(){return model;}public String name(){return name;}public InetAddress address(){return address;}public GoveeCapabilities capabilities(){return capabilities;}public Instant lastSeen(){return lastSeen;}public boolean online(){return online;}public Integer brightness(){return brightness;}public Integer onOff(){return onOff;}
    public void markSeen(){lastSeen=Instant.now();online=true;}public void markOffline(){online=false;}public void setBrightness(Integer b){brightness=b;}public void setOnOff(Integer o){onOff=o;}
    @Override public boolean equals(Object o){return o instanceof GoveeDevice d&&id.equals(d.id)&&address.equals(d.address);}@Override public int hashCode(){return Objects.hash(id,address);}
}
