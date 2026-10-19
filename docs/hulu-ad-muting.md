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

The installed native Hulu build targeted API 28 and declared no Network Security Config permitting user-added certificates. Android's default trust policy for that target excludes user-added CAs. Further static inspection found bundled native libcurl/OpenSSL and Hulu's Neutron/WebCore engine: the Android manifest alone does not establish which trust store that native networking uses. A bounded local CONNECT proxy compatibility probe observed no Hulu CONNECT traffic, so it did not establish a usable interception path. Its proxy settings and local forwarding were restored afterward; no CA was installed. HTTPS filtering remains unproven on this unrooted setup. That initial probe made no root, system trust-store or Hulu APK change. See the separate Lab follow-up below.

References: [Android network trust configuration](https://developer.android.com/privacy-and-security/security-config), [accessibility screenshot API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#takeScreenshot(int,java.util.concurrent.Executor,android.accessibilityservice.AccessibilityService.TakeScreenshotCallback)).

## Follow-up options — October 18, 2026

- The [Amazon GhostLock project](https://github.com/R0rt1z2/GhostLock) lists specific Fire tablet/Stick builds, rather than this Panasonic TV. The separate [GhostLock app's exact-kernel list](https://github.com/YuKongA/ghostlock-app/blob/main/docs/kernel_profiles/SUPPORTED_DEVICES.md) also had no matching profile. Similar kernel versions are not evidence of compatibility. Device-specific root work needs the exact kernel image, patch-state analysis, ABI validation and a recovery plan before execution. No exploit was run.
- [Morphe Android TV patches](https://github.com/ajstrick81/morphe-androidtv-patches) publishes a Disney+ ad patch, with compatible versions through 26.16.0 in the inspected revision, but no Hulu patch. The installed Disney+ 26.18.0 was outside that declared list. [Hulu on Disney+](https://help.hulu.com/article/hulu-is-hulu-going-away) could be an alternative for eligible accounts/content, but neither account eligibility nor ad removal through that path was verified. No client was replaced, downgraded or patched, and no profile was linked.

## Separate Hulu Lab — October 18, 2026

A private experimental copy of the installed Hulu 1.4.736 build was rebuilt, signed with a disposable development identity and installed alongside the original. Its package identity, provider authorities, label and background TV integration are isolated. Compatibility changes suppress the copied app’s Amazon Appstore launch workflow and preserve its original client user-agent identifier. The original package and signed-in data remain installed; Lab requires a separate official Hulu activation. No subscription entitlement or protected-video implementation was changed.

The compatibility build reached Hulu’s welcome and official activation screens on the TV without root. This establishes startup only; ad removal and protected playback in the modified copy remain unverified. The privately inspected public player bundle contains ad-related transitions, but static inspection does not establish that streamed ads can be skipped.

The original [Lab diagnostic](../tools/hulu-lab/README.md) classifies native request types without full URLs or account information. APKs, decompiled assets, device logs and account activation codes remain outside the repository.

The follow-up prototype pins the exact public player bundle by SHA-256 and selects its existing skip-pod transition when an on-demand ad segment is entered. Live and recorded timelines keep their original branch. The private APK serves its patched public-code asset over a loopback-only endpoint; the native hook redirects only the exact pinned script URL. Login, playback manifests, licenses and other requests retain their normal routes and trust settings. The TV loaded and executed this player, confirmed by a visible Lab badge, without a Mac-hosted server or root. That badge counts skip-branch entries, not successful ad-free viewing. Authenticated follow-up results are recorded below.

### Authenticated on-demand follow-up

The owner activated Lab separately from the original installation. In a single episode test, the Lab counter reached three skip entries during startup, then seven after seeking across the first marked mid-episode ad opportunity. The owner confirmed the episode was moving with normal sound after both startup and the seek across the mid-episode break. Native player controls showed advancing content time (01:06 → 01:20 initially and 13:39 → 13:56 after the seek), normal playback controls and no observed ad countdown in the sampled views. This supports successful startup and seek-across-break playback for the tested episode; it is not proof that every title, ad format or complete episode works.

Vista 0.9.4's Hulu Home tile opens Hulu Lab. The original Hulu installation and its account data remain intact. Raw captures, account/profile information and native playback logs remain private. No license handling or protected-video capture was modified.
