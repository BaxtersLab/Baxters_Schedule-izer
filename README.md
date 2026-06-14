# Baxters Schedule-izer

A personal Android productivity hub: calendar events, recurring bills, receipt/voice
capture, reminders, and a small on-device assistant. It is the mobile input surface
for a homelab setup — receipts are pushed to [Baxters Budget Blaster](https://github.com/BaxtersLab/Baxters_Budget_Blaster)
for OCR/accounting, and files are tunneled to the homelab PC via RemoteDexter.

- **Platform:** Android (Kotlin), `minSdk 26` / `targetSdk 34`
- **Package:** `com.baxter.schedulaizer`
- **Architecture:** Room (offline-first), manual DI, foreground service, WorkManager transfers

## Features

- **Calendar & Bills** — events and recurring bills stored in Room; money is integer cents (no floats).
- **Capture** — camera/gallery/audio; images can be sent to Budget Blaster for OCR.
- **Transfer** — pushes files to the homelab over the RemoteDexter native (`.so`) tunnel, with a staged fallback when the library isn't loaded.
- **Agent** — a text-and-voice assistant. Type or tap-to-talk ("what's due", "add bill Rent 1200 on the 1st", "add event Dentist tomorrow at 2pm"); replies are shown and spoken via Android text-to-speech. The mic is on-demand only (press to start, stops on silence or tap) — never always-on listening.
- **Spoken alerts** — reminders are read aloud (TTS) in addition to the notification.
- **Urgency engine** — an emoji badge that escalates with the number of pending items.

## Project layout

```
apk/                     Gradle project (open this in Android Studio)
  app/src/main/java/com/baxter/schedulaizer/
    data/                Room entities, DAOs, repositories
    alerts/              AlertScheduler / AlertReceiver (+ spoken alerts)
    transfer/            RemoteDexter JNI + Budget Blaster bridge
    voice/               IntentParser + AgentDispatcher
    ui/                  Fragments (calendar, bills, capture, transfer, agent, settings)
  app/src/test/          JVM unit tests (e.g. IntentParserTest)
module * block *.txt     Build/spec blocks (design source of truth)
```

## Building

Open `apk/` in Android Studio, or use Gradle directly:

```bash
gradle -p apk assembleDebug          # debug build
gradle -p apk testDebugUnitTest      # run unit tests
```

`apk/local.properties` (with your `sdk.dir`) is required and git-ignored.

### Release signing

Release builds are signed from an untracked `apk/keystore.properties`. Copy
`apk/keystore.properties.template`, fill it in, and generate a keystore:

```bash
keytool -genkeypair -v -keystore schedulaizer-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias schedulaizer
gradle -p apk assembleRelease
```

If `keystore.properties` is absent the release build still runs, just unsigned.
Release builds also enable R8 (`minifyEnabled` + `shrinkResources`); keep-rules live in
`apk/app/proguard-rules.pro`.

## Integration

- **Budget Blaster** — the Settings screen holds the receipt endpoint (default
  `http://<homelab-ip>:8082/receipt`); Budget Blaster must bind to the LAN for the
  phone to reach it.
- **RemoteDexter** — the encrypted Android→Windows file tunnel; this app reuses the
  prebuilt `.so` and does not modify RemoteDexter source. JNI loading is gated by the
  `ENABLE_JNI` build flag.

## Status

Pre-release. Compiles clean and unit tests pass; full runtime behavior (TTS audio,
microphone, alarms, receipt upload) should be verified on a physical device.
