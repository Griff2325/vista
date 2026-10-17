# Validation and performance

Measured on October 17, 2026 on one Panasonic W95A / AFTLAS01 running Fire OS 8.1.8.0. These are short development samples, not statistical or whole-TV benchmarks.

## Rendering results

| Implementation | Navigation frames | Janky frames | Jank rate | p90 frame time | Main-process PSS |
|---|---:|---:|---:|---:|---:|
| WebView conservatory prototype | 1,634 | 1,015 | 62.12% | 42 ms | 163,333 KiB |
| Static native Canvas redesign | 24 | 0 | 0% | 5 ms | 54,534 KiB |
| Native fall scene with motion | 340 | 3 | 0.88% | 5 ms | 57,570 KiB |

Method: reset `dumpsys gfxinfo`, send 24 alternating left/right D-pad presses with 120 ms sleeps between ADB calls, read graphics statistics. ADB call latency contributes to total sample length. WebView and fall animation continue rendering between keys, so their frame counts differ from the static implementation. The final fall layout also has different focus geometry. Results describe these specific implementations under a similar input sequence, not equal-sized frame populations or a proof of universal latency.

The native-static main process used about 67% less PSS than the old main process. The current animated version uses about 65% less (~159.5 MiB to ~56.2 MiB). The old WebView renderer was a separate process and is excluded from that comparison; do not call this a total system-memory measurement.

Three-second idle samples: WebView rendered 186 frames; static native rendered 0; animated native rendered 37, with 0 janky frames in the final fall sample. A separate earlier fall sample recorded 1,420 frames and 0 jank; the final navigation result above is the more recent result.

Hidden-scene check: after opening YouTube and waiting for transition settling, Sol's frame count stayed 174 -> 174 over three seconds. This verifies that the overlay stops drawing while the launcher Activity is paused in that scenario.

[Machine-readable summaries](evidence/measurements.json) include SHA-256 fingerprints of local source captures. Raw TV dumps are deliberately not published. The fingerprints identify the captures used; they do not independently authenticate the measurements. `scripts/measure.py` reproduces the input/idle sampling procedure against a selected ADB target.

## Device behavior

- All six installed favorites launched correctly and returned to Sol through the original helper during the native redesign.
- The final direct-Home prototype was tested with the physical remote. The owner confirmed Home avoided the Fire TV flash and arrows/OK/Back/volume worked after correcting the virtual remote's key capabilities.
- The installed non-expiring bridge uses the same forwarding path with a local supervisor. Its device discovery/keymap was inspected. Reconnection recovery is implemented but has not been physically tested.
- The original helper configuration resumed Netflix after a short software standby test. The final input bridge has not yet been independently verified across deep standby or full reboot.
- System Settings opens through `com.amazon.tv.launcher/.ui.MainSettingsActivity`.
- Input picker enumerated HDMI devices. Actual input switching was not exercised.
- Watch Next provider returned no progress records. Universal Continue Watching is not implemented or demonstrated.

## Remaining priority

The bridge does not restart after full TV reboot. Local ADB bootstrap from an ordinary app was closed by this firmware. Automatic startup requires further investigation. No root exploit or bootloader modification has been executed as part of this project.
