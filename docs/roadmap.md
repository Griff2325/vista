# Known limitations and priorities

## Top priority: seamless Home across reboot

The physical Home route works during a supervised bridge session, based on owner testing. The bridge does not automatically start after a full reboot. Establish a compatible on-device startup mechanism; if privileged access is necessary, investigate exact-firmware compatibility and recovery before attempting a device-specific root method. There is no verified root method for this TV in this repository.

The tested device is Panasonic W95A / AFTLAS01, lassen, Fire OS 8.1.8.0 RS8180.3742N, kernel 5.15.180-android13-8-gaaffd2df87b5-dirty. Root tools for another model, even on the same Fire OS major version, are not interchangeable. No bootloader unlock, downgrade, factory reset, core-package disable, or partition flash was performed.

## Further verification

- Physically verify Bluetooth disconnect/reconnect recovery, stuck-key behavior, deep standby/wake, and extended use.
- Measure input-to-visible-Home latency with a real recording; current evidence is owner-observed behavior rather than a millisecond latency claim.
- Verify long Home, voice/microphone behavior, branded app buttons and every Panasonic-specific remote key.
- Confirm actual HDMI switching and preservation of TV/ARC controls on the final bridge.
- Test the installer/build on another development machine. CI compilation does not establish device compatibility.

## Product improvements

- A user-selectable weather location and favorite apps.
- Verified service-specific progress sources for Continue Watching, respecting app/profile access. The standard provider was empty; all-platform coverage remains unimplemented.
- Pixel Autumn, Cincinnati day/night lighting, and October Halloween accents are implemented. Winter artwork remains future work; no automatic season-art switch or scheduled task has been configured.
- User-selectable reduced motion and accessibility refinements.
- Personal photos and optional local information/room integrations; not implemented in the current dashboard.
