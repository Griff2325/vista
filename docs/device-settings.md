# System menu animations

Disable the platform's three animation scales through an authorized ADB connection:

```sh
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

Read each value back with `adb shell settings get global <key>`. These are device-wide settings, covering platform window transitions and property animators in other apps too. Vista's Canvas scene motion is independent of these scales.

Save each original value before changing it. Restore a saved number with `settings put global <key> <value>`; if the original readback was `null`, use `settings delete global <key>` to restore the unset default. Keep device-specific recovery snapshots outside Git.

On the development TV, both settings readback and the window manager's live scales were verified as zero. The owner confirmed that the stock picture/settings panel **still slides slowly**. This configuration does not solve that panel's entrance animation.

The installed panel defines a separate view-tween entrance with a 500 ms position animation and 166 ms delay. Its exit and submenu transitions also have explicit timings. A timing-only resource overlay was built and tested, but Fire OS rejected installation because the target has no overlayable declaration and the overlay lacks the target's signing certificate. No overlay was installed. Android documents this restriction for targets without an overlayable declaration in its [resource overlay documentation](https://source.android.com/docs/core/runtime/rros).

Opening the legacy native `PictureSettings` activity directly was also rejected: the ADB shell lacks `com.amazon.tv.permission.LAUNCHER_SETTINGS`. Those initial animation/access checks changed no picture values or core packages. Vista retains the experimental replacement panel described below. In 0.10.2, the owner chose to restore Panasonic’s menu on a single Settings/Picture press; its stock animation is unchanged.

A separate read-only app probe confirmed that copying the UI does not supply the control permission: the app requested `com.fireos.sdk.ftve.USE_SDK`, but the permission remained denied and binding the hardware-settings service failed. A shell service-access check was also denied for the same permission. See the [DumbTV compatibility review](dumbtv-review.md) for the tested boundary and additional Home-fallback ideas.

## Rootless picture-control tests

Follow-up tests on October 19, 2026 found an alternate vendor interface on the Panasonic build RS8180.3742N. The Amazon SDK denial above does **not** establish that all picture-control interfaces require root.

A temporary sideloaded app with no requested permissions successfully bound the exported `com.mediatek.customerpq/.CustomerPqService`. Its Binder descriptor is `com.mediatek.extservice.pqutils.ICustomerPqService`; transaction 1 reads a string key and transaction 2 writes a string key/value. The probe validated the descriptor and the current source/mode before using those two operations. Other transactions were not exercised.

An independent, read-only hardware query through `vendor.mediatek.hardware.pq.V1_0.IPq.getGlobalNonAwarePqSetting` verified these changes and their restoration:

| Control | Original | Temporary test value | Restored |
| --- | --- | --- | --- |
| Picture mode | `pAutoAI` | `pNormal` | `pAutoAI` |
| Brightness | 50 | 51 | 50 |
| Contrast | 50 | 51 | 50 |

These are hardware-interface readbacks, not measurements of the displayed image. Tests used internal-app source `MM` with SDR reported while the probe was foreground. They do not establish HDMI/HDR playback behavior, other presets, correct native slider units, persistence across source changes or full restart, or whether a production overlay would preserve uninterrupted playback. The native brightness preferences use different values from this interface; do not expose the tested numbers as native slider units without a verified mapping.

The Panasonic provider `content://com.panasonic.dtv.shareddb2/Picture/ProtonViewingMode` also allowed the ordinary app to read and update its protobuf record. A native menu change from Auto AI to Normal changed both `viewing_mode/other/sdr/home` and `viewing_mode/other/hdr/home`. A guarded probe reproduced that two-field preference update and the lower-level mode write, preserving other fields and restoring the originals afterward. Saved preferences and hardware then reported Normal, but the freshly reopened native Picture page still displayed **Auto AI**. The native-menu synchronization assertion failed; this is an unresolved integration issue, not a passed replacement-panel test.

Each mutation had a private recovery record written first and an automatic restoration path. Final readbacks confirmed Auto AI, brightness 50 and contrast 50, and the temporary probe and hardware-query helper were removed. No root, reboot, reset, core-package disable, replacement panel or Settings-key remapping was used. Keep raw hardware output, native preference snapshots and vendor binaries outside the public repository.

### Vista quick settings panel

**Current default (0.10.2):** a single Settings/Picture press opens the original Panasonic quick menu. A held Settings/Picture press does the same; Home and held Home retain their existing behavior. The owner confirmed that the restored single press opens Panasonic’s menu. The optional custom controls below are retained, with their shell helper stopped on the development TV. Earlier interception tests are recorded here for context.

Vista 0.10.0 adds a focusable `TYPE_APPLICATION_OVERLAY` window instead of launching an Activity over the player. Its window animation is zero and its controls have no state-list animation. A foreground service exists only while the panel is open; it closes on Back, a repeated single Settings/Picture press, screen-off or 45 seconds without panel interaction. Home dismisses it when returning to Vista. Holding Home explicitly dismisses the overlay and opens full native settings.

The input bridge consumes remote scan codes 690 (Settings) and 645 (Picture) before Android dispatches them. In 0.10.1, a short press starts `PicturePanelService` through the authorized shell on release. Holding Settings/Picture for at least 800 ms and releasing dismisses Vista’s overlay and sends a Settings press/release through the existing uinput virtual remote, opening the original Panasonic quick menu. A double press is no longer a shortcut. The service's exported entry requires Android's `DUMP` permission; ordinary third-party apps cannot invoke it. The panel also needs display-over-apps access:

```sh
adb shell appops get com.griffin.solhome SYSTEM_ALERT_WINDOW
# Save the existing mode privately before granting access.
adb shell appops set com.griffin.solhome SYSTEM_ALERT_WINDOW allow
```

Restore the saved app-op mode to revoke this grant. The bridge installer checks the service and overlay grant before replacing the current bridge. Stopping the bridge restores stock remote dispatch; see [bridge operation](../bridge/README.md).

The owner confirmed that the first physical Settings press opened Vista and holding Home opened native settings. The experimental mode buttons changed the vendor state readback, but the owner reported no apparent picture change. Inspection then found identical generic preset parameters for `pAutoAI` and `pNormal`, apart from the mode identifier. This does not prove that the Panasonic image engine applied the requested preset. **The experimental write path and mode buttons were removed from the shipped panel.** That build provided only full TV settings and Close, with a hint for the held-Settings Panasonic picture shortcut. The vendor tests above establish writable state, not a working replacement picture backend.

Before removal, overlay UI tests exercised Auto AI/Normal state writes and restoration, Back, repeated service entry and the full native-settings link. YouTube stayed the resumed Activity underneath, which does not establish uninterrupted moving-video/audio in every app. The native quick-menu path is independently testable with a temporary virtual remote. The owner reported that the double-press shortcut failed, so it was replaced with held Settings in 0.10.1. Standalone `input` launches from the native helper did not reliably open the menu, including an empty-environment case that exited successfully. A direct app broadcast was also rejected because Fire OS protects the Settings action. The replacement uses the existing virtual remote, with the child waiting for Vista’s panel to close before emitting the native Settings key. The main remote-reading process continues forwarding input during that wait. Command launches also preserve the inherited Android environment. Unit checks execute the assembled ARM routing and launch arguments, including environment-pointer preservation, the 800 ms hold boundary, unmatched releases, Home/held Home and forwarding of navigation, volume and Alexa keys. A temporary ARM helper created the same virtual remote, closed Vista’s panel and emitted the production key packet; the original Panasonic menu opened and the helper was removed. The owner then confirmed that the physical held-Settings gesture opens the original Panasonic quick menu.

The native Panasonic client also references a loopback viewing-mode API. An unauthenticated read did not yield a response. After specific owner authorization, one authenticated read-only `getViewingMode` request also disconnected without a response. Listener inspection showed no TCP listener on the client’s configured local API port. No service was enabled and no API write was attempted; vendor authentication data is not bundled, logged or published. Native preset synchronization, HDMI and HDR control remain unresolved; the live SDR slider path below uses a separate native image-engine API.

No root, system-package disable or stock-resource modification is required for the panel/shortcut. The bridge still needs external ADB startup after a full OS reboot.

## Optional live picture controls (0.10.2)

The native Panasonic provider uses `IPq.setPqParamsByGlobal` with different keys from the earlier generic cache tests: `Luminance` for panel brightness, `Cont` for contrast, and `Col` for color. A shell probe changed native luminance from 100 to 60 and restored 100. The owner confirmed visible dimming and restoration, then confirmed brightness adjustment/restoration through Vista’s overlay and that the overlay remained fast. Contrast and color live values were read successfully; their visual effect has not been owner-tested.

The retained overlay offers those three 0–100 controls, Left/Right steps of 5, and restoration of the values read when opened. These are live adjustments, **not saved Panasonic presets**. Native mode or source changes may replace them. Only internal-app `MM`/SDR writes are accepted; the helper rechecks the source, HDR state and expected picture mode before writing and verifies the hardware readback. A failed write attempts to restore its previous value only if the source/mode still match. This is not a durable recovery mechanism after power loss.

An ordinary APK could not obtain the HIDL picture engine, while authorized ADB shell access worked. `bridge/picture/PictureBridge.java` therefore runs locally as the existing shell UID. It loads API classes from the installed Calman APK without bundling vendor code, credentials or binaries. Vista binds the read-only source-status interface and passes that Binder through its own `DUMP`-protected provider. The helper uses Android’s external-provider IPC; a shell process cannot bind an app service using a fabricated application context. No network listener is opened. The queue is single-request, expires writes after three seconds, matches replies to request IDs and checks shell UID as well as the manifest permission. Tests cover UID, source, range, key and expiry boundaries.

The owner subsequently chose Panasonic’s complete native modes over this limited replacement. The helper is optional and is **stopped by default on the development TV**, so it consumes no ongoing background resources. Keep the implementation available for later work:

```sh
bash scripts/build-picture-bridge.sh
bash scripts/picture-control.sh YOUR_ADB_SERIAL install
adb -s YOUR_ADB_SERIAL shell am start-foreground-service -n com.griffin.solhome/.PicturePanelService
# Stop the optional helper when finished.
bash scripts/picture-control.sh YOUR_ADB_SERIAL stop
```

Use the same Android SDK environment as `build-app.sh`. The helper is limited to the tested firmware and needs external startup again after a full OS reboot. Do not install a development-signed APK over an existing installation with a different signing identity. Temporary probes were uninstalled; private raw output and recovery files are excluded from Git.

## YouTube voice search

With YouTube open, hold the remote's Alexa button, speak a search such as “search for snowboarding videos on YouTube,” then release. The owner confirmed successful voice search on the installed client with the existing Home bridge active. No YouTube modification or new microphone service was needed.

The bridge already advertises and forwards the remote's Alexa scan code, 217, through the device-specific key layout. Preserve that capability when changing the bridge. Alexa handles recognition and the installed YouTube client handles the resulting search. YouTube documents the integration in [Control YouTube on TV with Amazon Alexa](https://support.google.com/youtube/answer/9394412?hl=en).
