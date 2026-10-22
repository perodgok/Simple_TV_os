# Xiaomi remote key audit — 2026-10-23

## TV and status
- Device: Xiaomi MiTV-MOOQ1 (tarzan), Android TV 11.
- Remote consumer-control input: `/dev/input/event2` ("Xiaomi RC Consumer Control"), Bluetooth VID 0x2717 PID 0x32b9.
- Keylayout reported by `dumpsys input`: `/vendor/usr/keylayout/Vendor_2717_Product_32b9.kl`.
- `adb shell cat` and `grep` on that vendor keylayout yielded **Permission denied** (unprivileged shell).
- HOME confirmed via `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`: `com.simpletvos.home/.MainActivity`.
- `pm list packages -d` includes `com.google.android.tvlauncher` (disabled, not removed).
- No system files modified during audit.

## Confirmed branded remote buttons

| Remote button | Linux EV_KEY | MSC_SCAN (HID usage) | Event device |
|---|---|---|---|
| Wink | `KEY_VCR` | `000c0092` | `/dev/input/event2` |
| KION | `KEY_PVR` | `000c009a` | `/dev/input/event2` |
| Rutube | `KEY_DATABASE` | `000c0189` | `/dev/input/event2` |
| Kinopoisk | `KEY_CONTROLPANEL` | `000c019f` | `/dev/input/event2` |
| Okko | `KEY_COFFEE` | `000c019e` | `/dev/input/event2` |
| IVI | `KEY_LOGOFF` | `000c019c` | `/dev/input/event2` |

Sources: manual `adb shell getevent -lt` captures, logs `remote-buttons.log` and `remote-unknown-buttons.log` provided during chat. These capture **Linux events, not yet Android KeyEvent keycodes**. For Okko and IVI only key-release events were observed in some windows; their identity is independently supported by scan codes and timing.

## Desired mapping (NOT implemented)
- HOME → Simple TV OS (keep existing behavior)
- Wink → Google TV Launcher (requires enabling Google Launcher; must ensure HOME stays Simple TV OS)
- KION → actual hardware backlight toggle 0 ↔ 50 (OEM interface must be validated; avoid setting backlight 0 until reliable recovery is confirmed)
- Rutube → standby timer choices 60 / 90 minutes (actual system standby, including HDMI/video, must be validated)
- Kinopoisk / Okko / IVI → unchanged.

## Next experiment: Android-level events, without root
1. Inspect permitted `dumpsys input` keylayout metadata, `cmd package` and accessibility service support.
2. Capture `adb logcat` while pressing each branded button to look for `KeyEvent`, launcher intents, manufacturer interception and activity starts. Redact tokens, IP-sensitive output if publishing.
3. If logcat does not expose events, create a small **foreground key-event diagnostic Android TV app** to print events it actually receives. A standard foreground activity cannot establish global interception, so do not conclude global remapping works merely from that test.
4. Consider AccessibilityService `onKeyEvent` with `FLAG_REQUEST_FILTER_KEY_EVENTS` only if supported and expressly enabled by user. Validate events delivered in other apps and assess conflicts with manufacturer shortcuts.
5. Do NOT attempt to write the restricted vendor `.kl` file, root the TV, or disable protected TV packages for this audit.
6. Preserve rollback commands in `docs/STATUS_AND_RECOVERY.md`.

## Read-only diagnostic snippet

```bash
TV="192.168.0.108:5555"
adb -s "$TV" get-state
adb -s "$TV" shell dumpsys input | grep -i -A 12 'Xiaomi RC Consumer Control' | head -n 35
adb -s "$TV" shell settings get secure enabled_accessibility_services
adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

**Status:** physical buttons identified; Android-level key dispatch and reprogramming feasibility **unverified**. No claim of working remap.


## Android framework keycode audit — 2026-10-23

Source: six per-button ADB logcat captures, full and filtered, supplied in chat.

| Remote | Android keyCode observed in `MiTVWindowPolicyInjector` and `GlobalKey` |
|---|---:|
| Wink | **197** |
| KION | **198** |
| IVI | **199** |
| Okko | **200** |
| Kinopoisk | **201** |
| Rutube | **203** |

Every key was logged by `MiTVWindowPolicyInjector: mKeyHandlerManager.handleActionNNN` followed by `GlobalKey: receive ACTION_GLOBAL_BUTTON keyCode = NNN`. Numerous roughly 50 ms repetitions of `GlobalKey` are present; the logs do not by themselves prove whether these were hardware repeats or a Xiaomi dispatch loop. Any future handler must debounce, and should treat ACTION_DOWN/repeatCount and ACTION_UP appropriately.

- `enabled_accessibility_services` = `null`
- `accessibility_enabled` = `0`
- HOME still resolves to `com.simpletvos.home/.MainActivity`
- `GlobalKey` repeatedly checks `com.mitv.livetv` and reports `installed:false`. This does NOT justify enabling or installing that app.
- During Okko capture, an Xiaomi updateservice guard periodically attempted to reconnect the disabled PatchWall `com.mitv.tvhome.atv` and failed; unrelated to confirmation of physical key mapping.
- No conclusive evidence yet that a third-party accessibility service can receive/consume keys globally, or that remapping of OEM keys works.

### Next controlled test
Build/install a diagnostic Android 11 AccessibilityService (`canRequestFilterKeyEvents=true`, `FLAG_REQUEST_FILTER_KEY_EVENTS`) and *manually* enable it in TV Accessibility Settings. Log only keyCode/action/repeatCount and show an on-TV/ADB readable report. First validate Wink with Simple TV OS foreground, then in a different app; don't launch Google Launcher or modify backlight until proven. The service must not consume keys during the first test, and it must protect against repeated events. Keep an easy disable/rollback procedure.
