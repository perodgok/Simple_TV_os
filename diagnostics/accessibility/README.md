# Android KeyEvent audit module (NOT YET INTEGRATED)

This source-only module targets Android TV 11, Java, no root. It is **not an APK** and has not been installed or tested. It is deliberately isolated from the working launcher until its precise source layout and manifest have been inspected.

## Integration into existing `com.simpletvos.home` app

1. Copy `RemoteKeyAuditService.java` into a matching package path inside your actual Java source root. Preserve package `com.simpletvos.diagnostics`.
2. Copy `remote_key_audit_service.xml` into Android `res/xml`.
3. Add to AndroidManifest.xml **inside the `<application>` element**:

```xml
<service
    android:name="com.simpletvos.diagnostics.RemoteKeyAuditService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/remote_key_audit_service" />
</service>
```

4. Add to `res/values/strings.xml`:
`<string name="remote_key_audit_description">Diagnose Xiaomi TV remote buttons; does not remap keys</string>`
5. Build and install the APK using the project's existing build process; do **not** replace HOME or enable Google Launcher.
6. On TV manually enable this service under Settings > Accessibility (settings labels vary by firmware). ADB `settings put secure enabled_accessibility_services` is deliberately avoided.
7. Capture with `adb -s 192.168.0.108:5555 logcat -s SimpleTVRemoteAudit:I '*:S'`, then press Wink and KION once while Simple TV OS foreground, and again from another app. Stop with Ctrl+C. Confirm whether down/up occur; repeated events are possible.
8. Rollback: disable the service in Settings > Accessibility. Existing HOME remains unchanged.

### Limitations
- Xiaomi's `GlobalKey` receives ACTION_GLOBAL_BUTTON for these keyCodes, but accessibility dispatch is **unproven**.
- Service returns `false`, so no key is consumed and nothing is launched.
- For eventual remapping, implement debounce and check policy restrictions first.
- App manifest and UI source from the active launcher must be verified prior to integration.

### Round icon clipping / left padding
The requested UI fix is distinct from remote diagnostics. The leftmost icon is clipped where its circular bounds meet the screen edge. In the **actual** layout source, add at least 20dp outer start padding to its containing row (not by shrinking the icon). Respect RTL using `setPaddingRelative(start, top, end, bottom)` or `android:paddingStart`, and disable `clipChildren`/ `clipToPadding` on the relevant parent only if necessary. Do not blindly patch without inspecting the actual MainActivity and view hierarchy.
