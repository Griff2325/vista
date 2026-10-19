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

Opening the legacy native `PictureSettings` activity directly was also rejected: the ADB shell lacks `com.amazon.tv.permission.LAUNCHER_SETTINGS`. No picture values or core packages were changed. Removing the remaining slide requires a privileged system modification or a replacement panel with working access to the TV's picture controls; neither is implemented here.

A separate read-only app probe confirmed that copying the UI does not supply the control permission: the app requested `com.fireos.sdk.ftve.USE_SDK`, but the permission remained denied and binding the hardware-settings service failed. A shell service-access check was also denied for the same permission. See the [DumbTV compatibility review](dumbtv-review.md) for the tested boundary and additional Home-fallback ideas.

## YouTube voice search

With YouTube open, hold the remote's Alexa button, speak a search such as “search for snowboarding videos on YouTube,” then release. The owner confirmed successful voice search on the installed client with the existing Home bridge active. No YouTube modification or new microphone service was needed.

The bridge already advertises and forwards the remote's Alexa scan code, 217, through the device-specific key layout. Preserve that capability when changing the bridge. Alexa handles recognition and the installed YouTube client handles the resulting search. YouTube documents the integration in [Control YouTube on TV with Amazon Alexa](https://support.google.com/youtube/answer/9394412?hl=en).
