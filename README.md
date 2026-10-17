# Sol Home

A quiet, native Android TV dashboard and an experimental direct-Home input bridge for a Panasonic Fire TV.

![Sol Home dashboard with app-logo tiles and an autumn forest window](docs/images/dashboard.png)

The dashboard uses native Android Canvas rendering, six app-logo tiles, a large autumn forest window, and a separately animated mist/leaf layer. This repository tracks an AI-assisted project built and tested on an actual TV. See the documentation for measured performance, design decisions, and current limitations.

Build: `bash scripts/build-app.sh` with JDK 17+, Android SDK platform 34, and build-tools 34.0.0. Set `ANDROID_SDK_ROOT` to your SDK. The script creates an ignored local development signing key; no installation key is published.

This is an experimental device-specific project. Automatic Home bridge startup after a full TV reboot is unresolved.
