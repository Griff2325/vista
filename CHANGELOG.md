# Changelog

Repository established October 17, 2026. Earlier development stages below are supported by retained snapshots and session measurements; they were not originally Git commits. All imported history uses the actual import date.

## 0.5.0 — October 17, 2026

- Renamed the dashboard and GitHub repository to Vista. Existing Android component identifiers are retained for in-place upgrades and bridge/permission compatibility.

- Distinct native Explore and Spotify vibes icons; installed app shortcuts retain their original logos.
- Eight Home tiles: six installed app shortcuts, Explore, and Spotify vibes; separate News briefs and This week reading controls.
- Cincinnati-local winter countdown to December 21 and detailed screensaver weather: timestamped current observations, available feels-like/humidity/wind, precipitation chance, and three forecast periods.
- Preserved the original tree texture and added a separate antialiased silk web tied to its existing branch coordinates, with a separately animated spider.
- Two-minute idle transition from the open window into a fullscreen scene, with a standard Android DreamService and a reversible Fire OS configuration helper. Set TV sleep to 60 minutes. The local Fire OS dream-route supervisor, like the Home bridge, needs restarting after a full reboot.
- Screensaver clock, Cincinnati forecast, publisher news briefs, one next-seven-days local events feed, and 24–72-hour snow estimates for three editable resort points.
- Variable native cloud silhouettes and bounded weather effects driven by Cincinnati NWS observations, with conservative stale-data behavior. Higher-resolution nearest-upscaled terrain preserves pixel edges during fullscreen mesh motion.
- Tightened the lake mask and kept side-bank vertices fixed so the right shoreline no longer deforms with the water.
- Falling leaves use integrated wind speed, fractional positions, flutter and edge fades to avoid snapped steps and wind-update jumps.
- October adds three variations of the original ghost with independent wandering/fade cycles, an occasional broom-riding witch with a sparse sparkle trail, a larger hanging left-tree spider and cached branch web, and a blinking black cat on the foreground rocks. The detailed foreground pumpkins remain; the small background lanterns were removed.
- Native movie/TV Explorer with optional personal TMDB catalog credential, US JustWatch availability, service/genre filters and a local saved list. Streaming sign-ins and private watch history are not aggregated.
- Compact Spotify vibe popout, editable Spotify links, and optional real local media-session controls through explicit Android notification access.

## 0.4.1 — October 17, 2026

- Removed the curtain, folds, and curtain rail from the room, leaving a clear open-window surround.

## 0.4.0 — October 17, 2026

- Selected Pixel Autumn from four illustrated concepts; generated separate terrain, trees, cloud, and Halloween assets with recorded prompts.
- Continuous rooted tree sway, cloud drift, water mesh/ripples, falling leaves, and an October roaming/blinking ghost. Replaced coarse 12 fps snapped movement with floating-point, vsync-paced motion at about 30 fps.
- Offline Cincinnati solar lighting and local calendar boundaries, with unit checks for seasonal elevation, midnight, and DST transitions.
- One open-window aperture with no crossing dividers, a deep sill and linen edge, a smooth recessed app rail, and a continuous utility console. Ambient mode preserves the contained window.
- Removed the unused photographic scene from the APK; retained historical design prompts and prototypes.

## 0.3.0 — October 17, 2026

- Native left-pane2x3 app-logo tiles and an approved autumn forest window.
- Separately rendered12fps mist/leaf overlay, paused when the Activity is hidden.
- Direct-Home evdev/uinput bridge: corrected virtual key capabilities after an initial stylus-classification failure; physical remote test passed.
- Local supervisor and ADB control/recovery tooling. Automatic startup after full reboot remains unresolved.
- Public source/build documentation, performance summaries, and preserved prototypes.

## 0.2.0 — saved development snapshot

- Replaced WebView with native Android Canvas rendering.
- Understated static alpine design and working direct system Settings shortcut.
- Six favorite app launches validated; major reduction in measured rendering cost.

## 0.1.0 — saved development snapshot

- WebView-based solarpunk conservatory dashboard with layered plants and animations.
- Favorite app launching, input/settings actions, NWS forecast, and read-only Watch Next integration.
- Accessibility helper targeted Vista; Home returned through Amazon's launcher and could flash.
