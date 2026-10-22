# Xiaomi Internals — actual routed key results (2026-10-23)

## Source
User pasted terminal output collected 2026-10-23 at 01:29, including read-only device inspection and Wink log stream. Some lines are shell paste errors (`zsh: no such file or directory: #/bin/bash`, shell comments rejected); diagnostics nevertheless ran. The complete individual `*.txt` outputs and collector zip were **not** supplied, so do not infer their contents from lines reporting only their filenames.

## Verified
- Android 11 build `Xiaomi/tarzan/tarzan:11/RTM4.220307.164/8277:user/release-keys`.
- HOME resolves to `com.simpletvos.home/.MainActivity`; Google Launcher disabled.
- AccessibilityService `com.simpletvos.home/com.simpletvos.diagnostics.RemoteKeyAuditService` is **enabled** and active (contrast with earlier snapshots).
- Registered receivers for `android.intent.action.GLOBAL_BUTTON` include:
  - `com.mediatek.tv.service/com.mediatek.hotkey.dispatcher.GlobalKeyReceiver`
  - `com.xiaomi.android.tvsetup.partnercustomizer/.GlobalKeyReceiver`
- Wink key press new log at 01:29:36:
  - `MiTVWindowPolicyInjector: mKeyHandlerManager.handleAction197`
  - `SimpleTVRemote: key=197 action=0 repeat=0`
  - `ActivityTaskManager: START ... cmp=com.google.android.tvlauncher/.MainActivity` from uid 10057 (Simple TV OS)
  - `SimpleTVRemote: Google Launcher unavailable: ActivityNotFoundException`
  - `ActivityTaskManager: START ... cmp=com.simpletvos.home/com.simpletvos.diagnostics.RemoteControlsActivity`
- Thus **our accessibility service receives Wink** and its action triggers. Failure is expected because Google Launcher disabled-user; fallback screen starts. The provided fresh snippet does not show a post-press Xiaomi GlobalKey log, but its absence from filtered output alone is not proof the OEM handler is fully suppressed.
- `/sys/class/backlight/backlight` directory exists, but no hardware values, ownership or verified OEM mapping have been supplied. **Do not write to this directory**.
- Actual standby method remains unknown.

## Root cause of launcher conflict
Separately captured dual-launcher test: enabling `com.google.android.tvlauncher` makes it resolve as HOME (priority 2); regular Simple TV OS app resolves with priority 0 even after set-home-activity returned Success. Per Android manifest documentation, positive intent filter priority for non-privileged applications can be capped to zero; simply setting a higher value in a user-installed APK is not a reliable fix. https://developer.android.com/guide/topics/manifest/intent-filter-element

## Technical options
- **Working now**: Accessibility filtering plus launch of enabled non-HOME apps for mapped keys. KION/Rutube custom control screens can be tested without system launcher changes. Check whether manufacturer's app is actually suppressed in a fresh controlled test.
- **Not supported simultaneously under established constraints**: permanently enabled original Google Launcher while Simple TV OS remains HOME on this Xiaomi, based on tested priority behavior. Repackaging Google launcher as a non-HOME component, privileged package control, or temporary enabling with a robust watchdog could work only with additional privileges/changes and carries risk. Don't promise this as already implemented.
- Preserve HOME and disabled Google Launcher until an explicitly chosen, demonstrated safe solution is available.
- Avoid disabling `com.mediatek.tv.service` or `com.xiaomi.android.tvsetup.partnercustomizer` (may affect TV hardware/hotkeys).

## Recovery
```sh
TV=192.168.0.108:5555
adb -s "$TV" shell pm disable-user --user 0 com.google.android.tvlauncher
adb -s "$TV" shell cmd package set-home-activity com.simpletvos.home/.MainActivity
adb -s "$TV" shell am start -n com.simpletvos.home/.MainActivity
```
