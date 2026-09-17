# GafiLeds

Fabric client-side Minecraft 1.21.11 mod for local Govee LAN control, with a low-latency screen-reactive lighting mode.

## Main feature

Run:

```text
/govee scan
/govee device list
/govee device select 1
/govee reactive
```

`/govee reactive` captures the selected Windows display locally, samples it into a tiny frame, finds a representative dominant RGB color from actual pixels, smooths the target over time, and sends the latest RGB value to the Govee over LAN. It intentionally avoids mapping the result to a small named-color palette.

## Requirements

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.18.1 or newer
- Fabric API 0.141.3+1.21.11 or newer
- Java 21
- Windows is the primary supported screen-capture platform
- A Govee device with LAN Control enabled and a LAN API compatible with the implementation

## Commands

```text
/govee scan
/govee device list
/govee device select <index|ip|device-id>
/govee status
/govee on
/govee off
/govee color <#RRGGBB>
/govee brightness <1-100>
/govee reactive
/govee reactive on
/govee reactive off
/govee reactive status
/govee reactive smooth <0.01-1.0>
/govee reactive threshold <0.0-1.0>
/govee reactive fps <1-30>
/govee reload
```

## Reactive pipeline

```text
Windows display
    -> low-overhead screenshot
    -> coarse grid sampling
    -> RGB bucket analysis
    -> observed RGB
    -> threshold
    -> target RGB
    -> time-based exponential smoothing
    -> latest-value update scheduler
    -> Govee LAN UDP :4003
```

A bounded three-frame store is used so the analyzer never builds an unbounded queue of stale images. The update scheduler also keeps only the newest pending color.

## Network protocol

The implementation uses the common Govee LAN API flow:

- discovery multicast: `239.255.255.250:4001`
- discovery replies: UDP `4002`
- control: unicast UDP `4003`
- RGB command: `colorwc` with `colorTemInKelvin: 0`

The code deliberately keeps discovery, protocol serialization, device capabilities, and scheduling separate so model-specific behavior can be added later.

## Screen capture

The current Windows backend uses Java AWT `Robot` off the Minecraft thread. It captures the selected monitor at native resolution and samples only a small deterministic grid into the internal frame buffer. The Minecraft window position is used at startup when `followMinecraftMonitor` is enabled.

Exclusive fullscreen support depends on what Windows exposes to AWT on the current graphics driver. Borderless/windowed modes are the preferred configuration.

## Configuration

Created automatically at:

```text
.minecraft/config/gafileds.json
```

Important reactive settings include capture FPS, analysis FPS, LED update FPS, sample resolution, smoothing, threshold, color weighting, and monitor selection.

## Privacy

Screen pixels are processed locally. Screenshots are not saved to disk and are not sent to any cloud service.

## Building

Preferred command:

```text
./gradlew clean build --no-daemon --max-workers=1
```

Windows:

```text
gradlew.bat clean build --no-daemon --max-workers=1
```

The repository includes a small self-bootstrapping Gradle launcher that downloads Gradle 9.6.1 into the user's Gradle cache when needed.

## Current limitations

- This release intentionally implements global single-color reactive lighting first.
- RGBIC segment streaming is not claimed until model-specific protocol behavior is verified.
- The project is LAN-first and does not require a Govee cloud API key for the implemented control path.
- Physical LED response time cannot be measured by the client and depends on the Govee device/firmware.
