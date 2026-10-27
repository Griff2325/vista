# Direct Home input bridge

This is a small ARM32 Linux syscall-only program, not an Android Accessibility Service and not a root exploit. On the tested Panasonic firmware, the already authorized ADB shell belongs to the `input` and `uhid` groups and is permitted to use the needed evdev/uinput operations. An ordinary APK does not have those privileges.

The program exclusively grabs the physical remote using EVIOCGRAB, recreates its supported keyboard buttons through uinput, and handles Home itself before Android sees a Home event. Short Home release starts Vista directly through `cmd activity start`. In 0.10.2, a single Settings/Picture release (scan codes 690/645) dismisses any Vista picture overlay and emits Settings down/up through the existing virtual remote to open the original Panasonic quick menu. A held press follows the same native route. The experimental Vista controls are retained but are no longer the default remote action. Double-press handling was removed after it failed owner testing. Holding Home for more than 800 ms starts the same service with `android.settings.SETTINGS`, dismissing any overlay and opening the existing full system Settings Activity. Other supported keys pass through. Capability bits and vendor/product IDs are specific to the tested remote's keymap.

The original all-key capability prototype was misclassified by Android as an external stylus. Restricting capabilities to the real buttons fixed forwarding. The owner then confirmed the Home path and other tested buttons worked. No high-speed screen recording or automated end-to-end physical-key assertion was captured.

The owner confirmed 0.10.0 single-press interception and held-Home escape. The owner confirmed that the 0.10.2 short-press restoration opens Panasonic’s menu; it uses the same native packet as the previously confirmed held-Settings path. The owner confirmed that the replacement held-Settings gesture opens the original Panasonic quick menu. Synthetic writes to the physical remote device were denied, so this confirmation comes from the physical remote. `python3 tests/test_bridge.py` executes the assembled key routing and launch arguments in a bounded A32 instruction model; unsupported instructions fail the test. This checks timing boundaries and forwarding branches, not physical remote delivery or Android scheduling. See [picture control validation](../docs/device-settings.md#vista-quick-settings-panel).

`supervisor.sh` discovers the physical remote through `getevent -lp`, starts the bridge, and retries after it exits. The virtual remote is named `Vista Remote`, so it is not selected as the physical source. Physical Bluetooth reconnection has not yet been tested. The supervisor PID guard is meant to avoid duplicate launches, not to provide a general hardened service manager.

## Build and run on the tested TV

```sh
bash scripts/build-bridge.sh
bash scripts/bridge-control.sh YOUR_ADB_SERIAL install
bash scripts/bridge-control.sh YOUR_ADB_SERIAL status
```

The installer deliberately checks `lassen` and build `RS8180.3742N`. It does not claim compatibility with other Fire TVs or remote variants. Vista 0.10.0 or newer must already be installed with display-over-apps access allowed; the installer verifies the panel component and grant before stopping the existing bridge. An ADB target must already be authorized. See the top-level build notes before trying to update an existing privately signed installation.

Stop and restore stock physical input handling:

```sh
bash scripts/bridge-control.sh YOUR_ADB_SERIAL stop
```

Closing the input descriptors releases the physical grab and destroys the virtual device. The Home on Fire accessibility fallback, if installed, is separate and may then resume redirecting Home with a flash. No Amazon package is disabled or removed by these scripts.

After a full TV reboot, the bridge must be started again over ADB:

```sh
bash scripts/bridge-control.sh YOUR_ADB_SERIAL start
```

**Automatic reboot startup is unresolved.** An app-origin local ADB connection was closed by this firmware even though the same request from an external computer received a normal AUTH response. A persistent privileged startup mechanism or a compatible device-specific root approach needs separate research. This repository contains no working root exploit or unlocked bootloader.

## Binary packaging

The build uses Clang only as an assembler. `pack-arm.py` packages the object as a small static ELF executable and resolves only same-text ARM_CALL/JUMP24 relocations, rejecting others. This is deliberately narrow, not a general linker. The resulting binary is roughly 4 KB and uses no libc, network connection, or per-key laptop round trip. The launcher inherits Android’s environment. The native Settings path does not use the Java input CLI: a child stops Vista’s overlay service, briefly waits for teardown and writes a Settings down/up packet to uinput. The main process keeps reading and forwarding remote input. Child launch output and Home markers go to the local log; other key contents are not logged.

## Native Input command (0.11.2)

`sol-home-bridge --inputs` creates a temporary virtual remote with the same tested capabilities, emits Input down/up (scan code 641), and exits. It never opens or exclusively grabs the physical remote. A short registration delay matches the on-device probe; this is separate from the persistent Home bridge. The existing shell control helper exposes only this fixed command through Vista's DUMP-protected provider, with request expiry and a child-process timeout. No network listener or system-package change is added.

Update the APK, native bridge and control JAR together; an older native binary cannot handle this command. Build/install the native bridge as above, then follow the [control-helper setup](../docs/device-settings.md#native-tv-input-menu). Both helpers still need external startup after a full reboot.

## Intentional native Home command (0.11.4)

`sol-home-bridge --fire-home` emits F12 through a temporary Generic-layout device. Home on Fire must map F12 to Amazon Home; a direct activity launch is otherwise redirected by its normal Home fallback. This one-shot mode preserves the physical remote's vendor layout and grab, and shares the Inputs command's registration delay, bounded helper execution and request expiry. Exact argument matching keeps both one-shot modes separate from the physical-device path. See [native Home setup and the HDMI quick-settings fix](../docs/device-settings.md#native-fire-tv-home-and-complete-settings-0114).
