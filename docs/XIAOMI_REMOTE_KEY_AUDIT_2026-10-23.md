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
