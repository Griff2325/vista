# System menu animations

The stock Fire TV picture/settings overlay uses Android property animators. Disable the platform's three animation scales through an authorized ADB connection:

```sh
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

Read each value back with `adb shell settings get global <key>`. These are device-wide settings, covering platform window transitions and property animators in other apps too. Vista's Canvas scene motion is independent of these scales.

Save each original value before changing it. Restore a saved number with `settings put global <key> <value>`; if the original readback was `null`, use `settings delete global <key>` to restore the unset default. Keep device-specific recovery snapshots outside Git.

On the development TV, all three settings read back as `0` after applying them. Physical remote timing and perceived responsiveness still need owner confirmation. Removing animation does not eliminate work the system panel performs while loading.
