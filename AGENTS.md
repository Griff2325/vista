# Project working instructions

Treat this repository as the canonical source for Vista. Commit and push completed, verified changes when the user authorizes continued project work; keep commits descriptive and do not fabricate earlier history. Do not force-push shared history.

Keep device addresses, account/profile data, private handoffs, ADB identities, signing keys, and signing passwords out of tracked files. Development signing keys belong in ignored .local/. Public build artifacts use a disposable development identity, not the installed TV's private key.

Prioritize a seamless Home button and reboot persistence. Preserve streaming, volume/ARC, HDMI and TV settings. Keep native rendering and the small, paused-when-hidden motion overlay. Do not reintroduce WebView or continuous full-screen video without measuring the impact.

Document device-specific validation and limitations honestly. A successful build is not proof of physical remote behavior. Do not run root exploits, flash partitions, reset the TV, or disable core packages merely to make a demo appear complete; evaluate compatibility and recovery, and obtain explicit authorization for destructive changes.

Use scripts/build-app.sh and scripts/build-bridge.sh. Read docs/validation.md before citing performance figures. Exclude generated build output from commits. Update the changelog and relevant docs when behavior changes.
