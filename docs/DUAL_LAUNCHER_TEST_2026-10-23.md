# Dual HOME launcher test — 2026-10-23

## Device
Xiaomi MiTV-MOOQ1 / tarzan, Android TV 11; Simple TV OS `com.simpletvos.home/.MainActivity`; Google TV Launcher `com.google.android.tvlauncher/.MainActivity`.

## Observed results (user-provided `launcher-dual-test-20261023-012226.log`)
1. Before test: HOME resolution `com.simpletvos.home/.MainActivity`, priority 0.
2. `pm enable --user 0 com.google.android.tvlauncher` succeeds.
3. `cmd package set-home-activity com.simpletvos.home/.MainActivity` returns `Success`, **but** `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME` now resolves to `com.google.android.tvlauncher/.MainActivity`, priority **2**.
4. Direct `am start -n com.google.android.tvlauncher/.MainActivity` succeeds and focus moves to Google launcher.
5. Simulated HOME while Google enabled keeps Google launcher focused/resumed.
6. Rollback `pm disable-user --user 0 com.google.android.tvlauncher` succeeds; `cmd package set-home-activity com.simpletvos.home/.MainActivity` returns Success; resulting HOME resolves to Simple TV OS (priority 0).
7. `cmd role holders` was unsupported (`Unknown command: holders`), so no inference about role ownership can be made.

## Conclusions
- Both launchers enabled simultaneously: Xiaomi/Google launcher wins HOME resolution on this device despite a `set-home-activity` Success response.
- Current safe baseline: Google launcher disabled-user, Simple TV OS HOME.
- Do not simply enable Google launcher permanently or tell user that `set-home-activity` guarantees HOME.
- The test establishes launcher *resolution and foreground behavior*, not the exact internal mechanism (intent priority, OEM policy, preferred activity, role policy). Priority=2 was observed, but root cause still requires checking intent filters and preferred activities.
- Accessibility remapping of branded keys remains a separate unverified question.

## Next experiments
- Read-only: inspect `dumpsys package com.google.android.tvlauncher` HOME intent filter and `dumpsys package com.simpletvos.home`; `dumpsys package preferred-xml` if supported; check `cmd role help` and `dumpsys role` without changing anything.
- Test alternative no-root approach on disposable/session basis: launching Google while temporarily enabling it and disabling upon return, with a robust rollback watchdog; avoid relying on this as a global HOME fix because it can take over HOME while enabled.
- Prefer a deliberately-built *non-HOME* Google interface alternative if true simultaneous stable HOME ownership is impossible.
- Never disable critical system apps or patch vendor `.kl` files blindly.

## Verified recovery
```bash
TV=192.168.0.108:5555
adb -s "$TV" shell pm disable-user --user 0 com.google.android.tvlauncher
adb -s "$TV" shell cmd package set-home-activity com.simpletvos.home/.MainActivity
adb -s "$TV" shell am start -n com.simpletvos.home/.MainActivity
adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```
