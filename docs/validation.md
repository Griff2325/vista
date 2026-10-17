# Validation and performance

Measured on October 17, 2026 on one Panasonic W95A / AFTLAS01 running Fire OS 8.1.8.0. These are short development samples, not statistical or whole-TV benchmarks.

## Rendering results

| Implementation | Navigation frames | Janky frames | Jank rate | p90 frame time | Main-process PSS |
|---|---:|---:|---:|---:|---:|
| WebView conservatory prototype | 1,634 | 1,015 | 62.12% | 42 ms | 163,333 KiB |
| Static native Canvas redesign | 24 | 0 | 0% | 5 ms | 54,534 KiB |
| Native photographic fall scene (0.3) | 340 | 3 | 0.88% | 5 ms | 57,570 KiB |
| Pixel Autumn open window (0.4) | 812 | 0 | 0% | 5 ms | 63,270 KiB |
| Pixel Autumn + widgets/media (0.5) | 834 | 0 | 0% | 6 ms | 92,669 KiB |

Method: reset `dumpsys gfxinfo`, send 24 alternating left/right D-pad presses with 120 ms sleeps between ADB calls, read graphics statistics. ADB call latency contributes to total sample length. WebView and fall animation continue rendering between keys, so their frame counts differ from the static implementation. The native layouts also have different focus geometry and animation rates. Results describe these specific implementations under a similar input sequence, not equal-sized frame populations or a proof of universal latency.

The native-static main process used about 67% less PSS than the old main process. Pixel Autumn 0.4 used about 61% less (~159.5 MiB to ~61.8 MiB). Version 0.5 adds higher-resolution scene caches, live cards and media integrations; its cold Home snapshot was 92,669 KiB (~90.5 MiB), higher than 0.4. The old WebView renderer was a separate process and is excluded from that comparison; do not call this a total system-memory measurement.

Three-second idle samples: WebView rendered 186 frames; static native rendered 0; animated native rendered 37, with 0 janky frames in the final fall sample. Pixel Autumn at approximately 30 fps rendered 94 frames with 0 jank in the three-second idle sample. A separate earlier photographic fall sample recorded 1,420 frames and 0 jank.

Hidden-scene check: after opening YouTube and waiting for transition settling, Pixel Autumn's frame count stayed 107 -> 107 over three seconds (the earlier photographic scene stayed 174 -> 174). This verifies that the overlay stops drawing while the launcher Activity is paused in that scenario.

[Machine-readable summaries](evidence/measurements.json) include SHA-256 fingerprints of local source captures. Raw TV dumps are deliberately not published. The fingerprints identify the captures used; they do not independently authenticate the measurements. `scripts/measure.py` reproduces the input/idle sampling procedure against a selected ADB target.

## Pixel Autumn checks

- Built and installed 0.4.0 with the existing private installation identity; the public portable development build and signature check also passed.
- Native scene textures report 2,757,040 retained decoded bytes (~2.63 MiB), including ghost sprites. Total process PSS includes much more than scene textures.
- Owner confirmed trees/clouds felt smoother after replacing snapped 12 fps movement with floating-point, vsync-paced motion at about 30 fps. This is qualitative owner feedback, not a frame-perfect input latency measurement.
- Actual TV captures inspected at day, dusk, night, and a November date. The latter removes ghost/pumpkins. Preview timestamps affect scene lighting only and were cleared afterward.
- Layout capture inspected: uninterrupted open aperture, smooth room surfaces, no app-row dividers, and a bottom utility console. Final live capture includes all six installed app logos.
- Pure Java checks pass for seasonal solar elevation, midnight darkness, dawn/dusk, Cincinnati October/November boundary, device-timezone independence, and DST continuity. They validate the intended approximation, not astronomical certification.
- Existing Home bridge process remained running after the app update. Physical remote Home behavior was verified earlier; this visual update does not establish full-reboot startup.

The 0.4.1 visual cleanup removes the curtain and its rail entirely. The updated APK was installed and the live layout inspected; scene animation and input routing are unchanged.

## Device behavior

- All six installed favorites launched correctly and returned to Vista through the original helper during the native redesign.
- The final direct-Home prototype was tested with the physical remote. The owner confirmed Home avoided the Fire TV flash and arrows/OK/Back/volume worked after correcting the virtual remote's key capabilities.
- The installed non-expiring bridge uses the same forwarding path with a local supervisor. Its device discovery/keymap was inspected. Reconnection recovery is implemented but has not been physically tested.
- The original helper configuration resumed Netflix after a short software standby test. The final input bridge has not yet been independently verified across deep standby or full reboot.
- System Settings opens through `com.amazon.tv.launcher/.ui.MainSettingsActivity`.
- Input picker enumerated HDMI devices. Actual input switching was not exercised.
- Watch Next provider returned no progress records. Universal Continue Watching is not implemented or demonstrated.

## Remaining priority

The bridge does not restart after full TV reboot. Local ADB bootstrap from an ordinary app was closed by this firmware. Automatic startup requires further investigation. No root exploit or bootloader modification has been executed as part of this project.

## 0.5 screensaver, widgets and media checks

- Portable development APK/signature verification, private in-place installation, bridge assembly, script syntax checks and pure Java tests passed. Tests cover solar/DST, feed scoping/entities/DTD rejection, snowfall interval coverage/units, weather interpretation, event expiry/calendar dates, Spotify URL restrictions, streaming request policy, and ghost path/fade continuity.
- Natural idle test: no bound dream at 116 seconds; actual SolDreamService bound at 124 seconds and still bound at 145 seconds. Home transition logged at two seconds before the system dream started. Amazon Ambient was not bound in those captures. Repeated manual re-entry on the final build also bound Vista, and a D-pad wake unbound it and returned to Home.
- `screen_off_timeout=120000`, `sleep_timeout=3600000`, and the disabled vendor Ambient selector stayed intact during the natural test. The setup script was rerun successfully with the original private rollback snapshot preserved. **The complete 60-minute sleep interval has not been observed.**
- 0.5 Home navigation sample (before the final tree-web/weather polish): 834 frames, zero jank, p90 6 ms. Three-second Home idle: 93 frames, zero jank, p90 5 ms. Actual fullscreen DreamService: 93 frames, zero jank, p90 5 ms in a separate three-second sample. These are approximately 30 fps observations, not a latency guarantee. An earlier 0.5 Home idle sample had 2 janky frames out of 92; short samples vary.
- In that sample, Home main-process PSS was 92,669 KiB; a later DreamService snapshot was 71,859 KiB. The latter follows app transitions, memory trimming and possible garbage collection, so do not claim fullscreen itself saves the difference. Retained scene bitmaps report 8,202,040 bytes (~7.82 MiB), shared across Home/Dream. This trades additional texture memory for crisper fullscreen sampling.
- Hidden check after opening YouTube: 110 → 110 rendered frames over three seconds. No background scene frames were added in that case.
- Actual TV captures inspected for Home, fullscreen cards, three original-style ghosts, left-tree spider, witch with sparkle trail, cat on rocks, new Explore icon and removal of the background lanterns. Right-bank vertices stay fixed and the water mask stays within the central lake. Leaf positions are fractional and integrate changing wind speed.
- Fresh NWS KLUK scene-observation source confirmed in the app log. All default snow points loaded: Perfect North had complete 48-hour coverage, Breckenridge and Alyeska 72 hours; each horizon is shown explicitly. No full coverage is reported as zero.
- Eight-tile Home layout and D-pad access to Explore/Spotify vibes passed. Explore no-credential state works; **no real authorized TMDB catalog result was tested**. API policy/unit checks do not prove account sign-in or entitlement aggregation.
- Owner explicitly approved Spotify notification access, and Vista's component was enabled with the supported Android shell command. The no-active-session panel was inspected; live transport behavior remains unverified because Spotify did not expose an active local session in that check. Default playlist names/IDs were checked against Spotify public oEmbed metadata. Generic search resolved an ambiguous song, so the final defaults use explicit playlists. Exact playlist navigation/autoplay is still app-dependent.
- Existing Home bridge remained running. This update does not re-establish physical-button behavior or full-reboot startup; the earlier owner-verified bridge path is unchanged. Both supervisors still require ADB bootstrap after full reboot.

- Final polish adds a separate smooth silk web over the original tree texture, larger spider, distinct Spotify vibes headphones icon, Cincinnati-local Dec 21 countdown, richer weather, and explicit AI-subject filtering. The original tree silhouette remains unchanged; the separate web is October-only. Unit checks cover NWS observation conversions and countdown date boundaries. Baseline navigation/memory samples above precede this small visual/data addition; subsequent fullscreen checks are recorded in the measurement JSON.
