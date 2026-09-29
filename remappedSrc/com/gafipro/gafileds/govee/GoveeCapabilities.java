package com.gafipro.gafileds.govee;

public record GoveeCapabilities(boolean rgb,boolean brightness,boolean segments){public static GoveeCapabilities fromModel(String model){return new GoveeCapabilities(true,true,false);}}
