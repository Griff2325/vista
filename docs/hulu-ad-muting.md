# Hulu ad muting

Optional Android 11+ accessibility service. Enable or disable it from Vista settings → Hulu ad muting. It starts off. Fire TV firmware that hides Android accessibility settings needs its already-authorized development setup to enable the specific service; existing accessibility services must be preserved.

This suppresses sound during detected ad breaks. It does not remove ads, shorten the countdown, modify Hulu, bypass protected video, or change account entitlements. DNS filtering is separate and is not configured by Vista.

## Detection and permissions

Android grants broad accessibility screen access. The implementation checks the active window package, requests a screenshot only while `com.hulu.plus` is foreground, crops the yellow Ad badge, and immediately releases the full hardware buffer. Only the small badge pixels are inspected in memory. No screenshot, UI text, viewing history or credentials are saved or transmitted. There is no OCR library, video decoding or network request.

The detector checks both the yellow background and dark glyph anchors. It is calibrated to Hulu's current 16:9 TV badge placement. A Hulu redesign, display/layout change, or unavailable accessibility screenshot can prevent detection. Failures release any owned mute and back off. An in-flight capture has a four-second timeout. Successful sampling waits one second between captures; sound returns after two consecutive samples without the badge.

The service runs in a separate `:hulu_mute` process so keeping accessibility available does not pin the launcher's scene textures. It inspects window state changes, not text/content-change events; it neither filters nor injects remote keys. Sampling stops outside Hulu and resumes on a foreground-window event.

## Audio ownership

Uses `MODIFY_AUDIO_SETTINGS` and Android's idempotent `ADJUST_MUTE` / `ADJUST_UNMUTE` operations on the music stream. It never sets or restores a numeric volume. A pre-existing mute is left alone. A detected manual unmute or underlying volume change relinquishes ownership for that break. Android returns zero from `getStreamVolume` while muted on the tested TV, so the policy also observes the system's music-volume broadcasts.

Leaving Hulu, capture errors/timeouts, normal service destruction, and the settings Turn off action release only a mute owned by the service. A sudden process kill while muted may require unmuting with the remote; normal destruction callbacks are not guaranteed after a crash. Physical ARC/soundbar behavior still requires owner listening validation.

## Filtering investigation

The installed native Hulu build targeted API 28 and declared no Network Security Config permitting user-added certificates. Android's default trust policy for that target excludes user-added CAs. A bounded local CONNECT proxy compatibility probe observed no Hulu CONNECT traffic, so it did not establish a usable interception path. Its proxy settings and local forwarding were restored afterward; no CA was installed. HTTPS filtering remains unproven on this unrooted setup. No root exploit, system trust-store change or Hulu APK modification was attempted.

References: [Android network trust configuration](https://developer.android.com/privacy-and-security/security-config), [accessibility screenshot API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#takeScreenshot(int,java.util.concurrent.Executor,android.accessibilityservice.AccessibilityService.TakeScreenshotCallback)).
