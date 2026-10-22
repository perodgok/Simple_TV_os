# Simple TV OS Classic: shortcut controls — 2026-10-23

## Release artifact
Local downloadable source/install archive in ChatGPT conversation: `SimpleTVOS-1.0-classic-buttons-install.zip`. This GitHub note is a release record, **not a copy of the binary**. The archive contains full Classic source and Mac build/install command.

## Changes included in archive
- Existing Classic left inset 24dp preserved for favorite rows (prevents circular icon clipping).
- New accessibility service `com.simpletvos.diagnostics.RemoteKeyAuditService` requests key-event filtering when explicitly enabled in the TV settings. Mapping is based on confirmed Xiaomi GlobalKey logs.
- Wink code 197: attempts explicit activity `com.google.android.tvlauncher/.MainActivity`. Install command enables Google Launcher if Simple TV OS currently resolves as HOME, then checks/restores HOME if needed.
- KION code 198: opens backlight control page; true Xiaomi backlight 0/50 is **not implemented** because the hardware API hasn't been identified safely. Controls clearly mark this.
- Rutube code 203: opens 60/90-minute timer. At expiration AlarmManager broadcasts to a receiver which asks active accessibility service to perform `GLOBAL_ACTION_LOCK_SCREEN`. This is **experimental**, not verified to put Xiaomi TV in actual standby or to interrupt HDMI playback.
- IVI code 199, Okko code 200, Kinopoisk code 201: preserve default handling.
- Action-down repeat protection and ~1100ms debounce.
- No removal of PatchWall packages or changes to the HOME activity through the app itself.

## Critical outstanding tests
- Whether Xiaomi `MiTVWindowPolicyInjector` / `GlobalKey` allow a third-party AccessibilityService to observe and consume manufacturer keys across apps.
- Whether Google TV Launcher can be launched from an explicit activity without Android HOME resolver becoming active.
- Whether `GLOBAL_ACTION_LOCK_SCREEN` acts like TV standby on this firmware (may merely lock Android).
- Discover genuine OEM backlight adjustment interface before activating 0/50 values.

## Build / install
On macOS Monterey MacBook M1 with Android SDK API 35 and Build Tools 35.0.0, extract the ZIP to Downloads, then:

```bash
bash "$HOME/Downloads/SimpleTVOS-1.0-classic-buttons/УСТАНОВИТЬ_НА_ТВ.command"
```

Requires existing signing keystore `~/simple-tv-os/keys/simple-tv-os.jks`. TV serial defaults to `192.168.0.108:5555`; can override via TV env var. Accessibility service must be enabled manually on the TV.

## Rollback
Turn off Simple TV OS accessibility service in TV Settings > Accessibility. Existing HOME remains Simple TV OS. Once confirmed, Google Launcher may be disabled with:

```bash
adb -s 192.168.0.108:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
```

**Status**: ZIP created and integrity checked. APK must be compiled/signed on user's Mac and remains untested on TV. These controls are not represented as confirmed working.
