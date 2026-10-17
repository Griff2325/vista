# Direct Home input bridge

This is a small ARM32 Linux syscall-only program, not an Android Accessibility Service and not a root exploit. On the tested Panasonic firmware, the already authorized ADB shell belongs to the `input` and `uhid` groups and is permitted to use the needed evdev/uinput operations. An ordinary APK does not have those privileges.

The program exclusively grabs the physical remote using EVIOCGRAB, recreates its supported keyboard buttons through uinput, and handles Home itself before Android sees a Home event. Short Home release starts Vista directly through `cmd activity start`; holding for more than 800 ms opens the existing system Settings Activity. Other supported keys pass through. Capability bits and vendor/product IDs are specific to the tested remote's keymap.

The original all-key capability prototype was misclassified by Android as an external stylus. Restricting capabilities to the real buttons fixed forwarding. The owner then confirmed the Home path and other tested buttons worked. No high-speed screen recording or automated end-to-end physical-key assertion was captured.

`supervisor.sh` discovers the physical remote through `getevent -lp`, starts the bridge, and retries after it exits. The virtual remote is named `Vista Remote`, so it is not selected as the physical source. Physical Bluetooth reconnection has not yet been tested. The supervisor PID guard is meant to avoid duplicate launches, not to provide a general hardened service manager.

## Build and run on the tested TV

```sh
bash scripts/build-bridge.sh
bash scripts/bridge-control.sh YOUR_ADB_SERIAL install
bash scripts/bridge-control.sh YOUR_ADB_SERIAL status
```

The installer deliberately checks `lassen` and build `RS8180.3742N`. It does not claim compatibility with other Fire TVs or remote variants. The APK must already be installed; an ADB target must already be authorized. See the top-level build notes before trying to update an existing privately signed installation.

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

The build uses Clang only as an assembler. `pack-arm.py` packages the object as a small static ELF executable and resolves only same-text ARM_CALL/JUMP24 relocations, rejecting others. This is deliberately narrow, not a general linker. The resulting binary is roughly 2.3 KB and uses no libc, network connection, or per-key laptop round trip. Child launch output and Home markers go to the local log; other key contents are not logged.
