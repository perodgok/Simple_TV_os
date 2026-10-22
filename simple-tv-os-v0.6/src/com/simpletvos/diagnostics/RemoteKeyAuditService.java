package com.simpletvos.diagnostics;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Read-only Android TV remote audit. Never consumes the event.
 * Enable manually in Settings > Accessibility after installing an APK
 * that declares this service. A service enabled on the OS might still
 * not receive vendor-global keys.
 */
public final class RemoteKeyAuditService extends AccessibilityService {
    private static final String TAG = "SimpleTVRemoteAudit";
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
    @Override protected boolean onKeyEvent(KeyEvent event) {
        final int key = event.getKeyCode();
        if (key == 197 || key == 198 || key == 199 ||
            key == 200 || key == 201 || key == 203) {
            Log.i(TAG, "keyCode=" + key +
                " action=" + event.getAction() +
                " repeat=" + event.getRepeatCount() +
                " source=" + event.getSource() +
                " deviceId=" + event.getDeviceId());
        }
        return false; // IMPORTANT: never intercept during diagnostic!
    }
}
