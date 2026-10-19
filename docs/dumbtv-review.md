# DumbTV compatibility review

Reviewed October 19, 2026, against [humblemuzzu/dumbtv at ca97f1c](https://github.com/humblemuzzu/dumbtv/tree/ca97f1c1010c3cafe4432edf1d0e627649596489). This was a source review; its installers and configuration scripts were not executed on the development TV.

## Useful leads

| Mechanism | Relevance to Vista | Validation still needed |
| --- | --- | --- |
| Accessibility window observer, temporary cover and alarm-launched Home | A possible fallback after a full reboot stops the shell input bridge. Home on Fire already supplies a related window-observer fallback; the alarm launch is the additional lead. | Fire OS 8 app-launch behavior, real remote timing, settings/Back preservation and a full restart. |
| Per-package background app-ops | Potentially restrict selected telemetry services without removing whole system packages. | Package dependencies, actual background resource use and streaming/voice behavior on this model. |
| Snapshot-based restoration | Supports reversible, device-specific configuration. | Preserve unrelated settings and app-ops; do not substitute generic defaults. Vista already uses private recovery snapshots for its device configuration. |

The [Home guard](https://github.com/humblemuzzu/dumbtv/blob/ca97f1c1010c3cafe4432edf1d0e627649596489/launcher/src/tv/dumbtv/home/Guard.java) reacts after Amazon Home appears, covers it and schedules its own Activity through `AlarmManager`. The [installer](https://github.com/humblemuzzu/dumbtv/blob/ca97f1c1010c3cafe4432edf1d0e627649596489/launcher/install.sh) changes the device-wide `alarm_manager_constants` minimum futurity. This is a different mechanism from Vista's evdev bridge, which intercepts Home before Android dispatches it. It does not restart that bridge or establish equivalent seamless behavior on this TV.

## Picture-settings limitation

DumbTV's [settings button](https://github.com/humblemuzzu/dumbtv/blob/ca97f1c1010c3cafe4432edf1d0e627649596489/launcher/src/tv/dumbtv/home/Home.java) opens the native settings Activity using `MAIN` and `LEANBACK_SETTINGS`. It does not implement a replacement hardware picture-control API.

A temporary, separate read-only probe requested `com.fireos.sdk.ftve.USE_SDK` and attempted to bind the installed picture-control service. On the Panasonic build RS8180.3742N:

- The ordinary app's permission check was false; service binding was denied.
- An ADB shell service-access check was also rejected, explicitly requiring that permission.
- The installed permission is `signature|preinstalled`, so requesting it from a sideloaded app does not grant it.
- The temporary probe was removed; no picture values were changed.

Redirecting the remote Settings/Picture keys and drawing an instant native panel are feasible with the existing bridge and app. The tested backend would still reject that panel's picture-control commands. No replacement panel or key remapping was deployed without a working control path.

## Compatibility cautions

The repository's full pass disables voice settings, blocks Alexa endpoints and includes `com.amazon.firebat` in its removal list unless its keep configuration preserves Prime Video. Those defaults conflict with Vista's current streaming and voice-search setup. Its primary end-to-end test device uses Fire OS 7; it is not evidence of Panasonic Fire OS 8 compatibility.

Its [settings list](https://github.com/humblemuzzu/dumbtv/blob/ca97f1c1010c3cafe4432edf1d0e627649596489/data/fireos/settings.txt) reports a Hisense Fire OS 8 menu failure with animator scale 0.5. A read-only navigation check on the Panasonic at its current scale 0 successfully opened the Picture page and exposed Picture Mode, Auto AI, Ambient Sensor, Brightness, Color, Clarity and Motion. No picture value was changed. This is one navigation check, not full menu coverage.

## Standby and full restart

Vista and its saved configuration remain installed across a restart. The shell Home bridge and dream-route supervisor are processes that require an external ADB bootstrap after a full OS reboot. Ordinary remote power-off generally enters standby, retaining these processes. The enabled Home on Fire service supplies a separate fallback; its post-reboot behavior has not been physically revalidated in this review. Standby success does not establish full-reboot startup.

No root exploit, reboot, system package change, DNS change or accessibility-service replacement was performed during this review. No third-party source was incorporated into Vista. DumbTV's code is MIT-licensed; any later reuse must retain the required notice.
