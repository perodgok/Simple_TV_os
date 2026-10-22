# Xiaomi remote shortcuts: mechanism and next targeted inspection (2026-10-23)

## Observed on user's device
- Xiaomi MiTV-MOOQ1 tarzan, Android TV 11; Bluetooth consumer control /dev/input/event2.
- Confirmed Android key codes: Wink 197, KION 198, IVI 199, Okko 200, Kinopoisk 201, Rutube 203.
- Logcat reports MiTVWindowPolicyInjector.mKeyHandlerManager.handleAction197 and GlobalKey ACTION_GLOBAL_BUTTON keyCode=197.
- On keyCode 197, previous user log showed Google Play deep link to ru.rt.video.app.tv with referrer=patchwall (unexpected mismatched target; avoid assuming key routing).
- AccessibilityService registered in Simple TV OS APK but settings showed accessibility_enabled=0, enabled_accessibility_services=null at last check.
- Google launcher when enabled wins Android HOME resolve priority=2 against Simple TV OS priority=0, even when set-home-activity returns Success. Rollback restores Simple TV OS HOME.

## AOSP mechanism (reference, not yet proof of Xiaomi exact file)
- Global keys may be configured in framework overlay res/xml/global_keys.xml and dispatched as android.intent.action.GLOBAL_BUTTON to a designated BroadcastReceiver, bypassing the current foreground app. Source: https://android.googlesource.com/device/google/atv/+/6e746e3/overlay/frameworks/base/core/res/res/xml/global_keys.xml
- AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS filters key events before **apps**, not necessarily framework global keys. Source: https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo
- The Xiaomi system may additionally route buttons in MiTVWindowPolicyInjector / OEM app. Actual receiver/component/manifest must be verified on device.

## Research required before changing shortcuts
Collect in a single read-only pass: current HOME and preferred activities; active accessibility state; dumpsys input; package receiver mappings for ACTION_GLOBAL_BUTTON; inspect accessible global_keys.xml; identify running GlobalKey process; foreground startup of Wink; OEM backlight and standby interfaces (enumeration only). Then select a narrowly-scoped fix with rollback. Do not disable broad TV center/input/power management packages; do not edit vendor keylayout on production TV.

## User goal
- HOME -> Simple TV OS Classic, always
- Wink -> Google Launcher **without changing HOME** (Google must not be permanently enabled until HOME conflict solved)
- KION -> hardware backlight 0/50, only via confirmed OEM mechanism
- Rutube -> actual power standby timer 60/90, not unverified Android lockscreen
- IVI/Okko/Kinopoisk unchanged

Report collector made as downloadable chat attachment: ИССЛЕДОВАТЬ_XIAOMI_КНОПКИ.command. It makes read-only ADB queries and writes a local ZIP, with a live Wink key log. No packages changed. Never publish complete raw logs without reviewing secrets.
