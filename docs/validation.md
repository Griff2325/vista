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

## 0.5.1 ripple update

- Replaced the shifting lake texture and sliding streaks with bounded expanding/fading arcs over stationary terrain. Wind advances a shared integrated phase instead of multiplying total uptime, removing the wind-change acceleration mechanism. Four Home ripple systems and six fullscreen systems reuse the existing scene; no extra decoded textures or per-frame Java objects are introduced by the ripple renderer.
- Pure Java checks cover wind changes after long uptime, phase wrapping, a long hidden interval, ripple lifetime bounds and invisible reset endpoints. All scene/data checks, the public portable APK build and the private installation build passed. Updated APK installed successfully on the TV.
- Owner found the first ripple version too faint; contrast, stroke width and ring radius were increased without changing the phase rate. Final Home and Activity-fullscreen captures were inspected. Owner confirmation of the final visibility adjustment remains pending.
- Final three-second idle samples: Home 93 rendered frames, one janky frame (1.08%), p90 9 ms; Activity fullscreen 98 frames, zero jank, p90 12 ms. Home PSS snapshot: 96,311 KiB. These short samples are approximately 30 fps observations, not a sustained performance or memory-saving claim. Raw captures remain private; fingerprints are in the measurement summary.
- Fullscreen was exercised using the Activity's temporary idle-preview extra, then a plain Home intent restored the normal two-minute timing. This update did not repeat the natural system-dream/full-reboot or physical Home-button checks; those limitations above remain.


## 0.6.0 seasons and occasions

- Calendar/palette policy checks cover all four fixed season boundaries, holiday windows, Thanksgiving across 2020–2039, New Year rollover, annual event ordering, leap-day recurrence across 2100, invalid dates, Cincinnati midnight/DST and preservation of transparent pixels, blue water and dark bark. All existing scene/data checks and public/private APK builds passed.
- Installed and inspected spring, summer, winter/snowboard, Christmas day/night, Valentine’s, St. Patrick’s, Earth Day, Thanksgiving, New Year and July Fourth previews on the TV. Original autumn/Halloween remains the automatic October scene. Date-preview captures keep the real clock/weather; they are not forecasts for their simulated dates.
- Native menu selection, season/occasion overrides and Restore Automatic were exercised. Annual-event entry rejected February 30, saved a temporary event under Today, and removed it through Manage. TV keyboard dismissal and subsequent D-pad field navigation were checked with ADB-generated input; this is not new owner-confirmed physical-remote validation. No personal test event remains.
- The curved Canvas ripple path showed sustained native/GPU growth in a separate diagnostic sample (118,293 → 159,723 KiB PSS over about 16 seconds). Disabling ripple drawing kept a comparison sample near 103 MiB. The final renderer approximates each shallow arc with eight precomputed line segments, retaining the same phase/fade and original tree/forest meshes. Its settled samples were 102,476 → 102,512 → 102,985 KiB at five-second intervals. This identifies a problematic drawing path on this device; it does not establish a general Android driver diagnosis or an endurance guarantee.
- A lazy cache retains at most one read-only texture set per season, with common assets shared. Active decoded images remain 8,202,040 bytes; the full four-season cache is 30,444,856 bytes. Final repeated-preview cycles measured 173,516 → 171,392 → 171,380 KiB PSS after all four looks were warm. This is a deliberate bounded cache/memory tradeoff, not a claim of equal process RAM to a cold single-season launch. No video or per-frame recoloring is used.
- Final three-second idle checks: Home 95 frames, zero jank, p90 7 ms; actual system DreamService 95 frames, one janky frame (1.05%), p90 6 ms. Normal two-minute inactivity bound Vista with the saved Winter selection (`mCurrentDreamIsTest=false`). Winter fullscreen PSS was 161,711 then 160,450 KiB thirty seconds later. Captures were inspected for winter artwork, snowboard and live widgets. Short samples do not establish all-day stability.
- Restored the saved season/occasion selection to Automatic and cleared temporary date previews after testing. The synthetic annual event was removed. Full-reboot startup, renewed physical Home-button behavior and a complete 60-minute sleep interval were not retested; existing limitations remain.

## 0.7.0 wildlife and phone companion

- Pure Java checks cover wildlife fade/route continuity, calm-weather gates and long uptime, plus loopback HTTP authentication, one-use/expiry/attempt limits, pairing replacement, revocation, Origin/Host enforcement, duplicate fields/headers, bounded requests, fixed routes and rate limiting. Existing scene/data checks and public/private Android builds pass. The TV update preserves its installed signing identity; no bridge, root or system-settings changes are part of this release.
- The native SwiftUI project builds without signing for the iOS simulator. Client tests cover private IPv4 endpoint validation, rejecting nonlocal/credential-bearing destinations, form encoding for literal plus/ampersand text, bearer/Origin headers, 401 revocation and local unpair when the TV is unavailable. Redirect refusal is implemented in URLSession's delegate; the endpoint/form/auth paths are exercised by the URLProtocol fixture.
- The native app ran in a dedicated iPhone simulator and paired with the real TV using its code. A native Right button returned success and a search for a synthetic test title arrived in Explore's actual TV field; the native winter selector changed the TV scene. This is simulator-to-TV validation, **not installation on the owner's physical iPhone**. The simulator used manual address entry. Physical camera/QR permission/scanning, iPhone Local Network permission, signing, installation and long standby/background behavior remain hardware checks.
- The browser fallback was inspected at a 390×844 viewport and exercised against the TV for pairing, D-pad focus, typed Explore search, YouTube launch, season selection and Automatic restoration. Spotify's no-active-session response was inspected; no actual track transport result is claimed. A background Home launch returned no foreground change under Fire OS, so the final service guards app-launch commands when no Vista Activity has focus and reports the physical-remote requirement. It does not request overlay/accessibility permissions or change the Home bridge to hide that restriction.
- A live winter wildlife observation sample rendered 5,003 frames with 20 janky frames (0.40%) and p90 6 ms. PSS was 135,398 → 129,865 KiB over the sample. The sample includes ADB captures and occasional D-pad input to prevent idle, with the optional companion enabled; it precedes the final small rider-placement adjustment. It is not an all-day memory or whole-TV benchmark. Wildlife adds no decoded scene images; the existing bounded seasonal cache is unchanged.
- Inspected actual TV captures of passing birds, the fox on near-bank rocks, and the small winter rider. Final placement captures use temporary daylight and fixed wildlife phases, clearly labeled Winter preview; an Activity fullscreen transition with the live clock/widgets was also inspected. These frozen-phase captures verify placement, not animation timing. The earlier live observation verifies sightings occur over time.
- Final three-second Home sample after placement polish: 93 frames, zero jank, p90 7 ms; PSS snapshot 153,786 KiB after Home/fullscreen/season transitions. In a separate hidden check behind YouTube, rendered frames stayed 125 → 125 over three seconds. This is a main-process snapshot and short lifecycle check, not an incremental-memory attribution for the companion. The natural two-minute system-dream route, full reboot, fresh physical Home validation and full 60-minute sleep were not repeated. Automatic season/occasion selection and normal scene time were restored; no fixed wildlife phase remains selected.

## 0.8.0 coding usage

- Pure Java and Python checks cover numeric bounds, reported zero versus unavailable values, duration-based labels, expired-window fallback, saved/future timestamps, malformed source data, allowlisted fields, failed-refresh age preservation, private snapshot file mode and positive TV-publish acknowledgment. Shell/own-UID import policy rejects other app, system and root UIDs. Existing scene, data and companion tests and public/private Android builds pass.
- On this Fire OS version, ADB cannot write the app-specific external-files directory. The final implementation uses a permission- and caller-UID-guarded ContentProvider call. A temporary ordinary app was installed and its actual content call was denied with SecurityException; the test app was then uninstalled. Authorized shell import succeeded. No credentials or new network listener are installed on the TV.
- A real Codex account-limit observation reached the TV, appeared on Home, and survived an in-place APK update through the private validated cache. The collector made only initialization and account-limit RPC calls, with no model turn. The existing Claude settings were preserved when installing its status-line hook. Claude Code remained signed out during validation: parsing/unknown-state handling is tested, but **an actual authenticated Claude quota observation remains pending** after the owner's sign-in and normal response. No fixture percentages were installed as account data.
- Inspected actual TV Home, remote-focus detail panel, Activity fullscreen and natural system DreamService captures. After normal two-minute inactivity, `SolDreamService` was bound with `mCurrentDreamIsTest=false` and showed the shared usage card. D-pad panel access was exercised with ADB input; renewed owner physical-remote validation is not claimed.
- Short three-second samples: Home 92 frames, zero jank, p90 7 ms, PSS 112,933 KiB; actual DreamService 96 frames, zero jank, p90 8 ms, PSS 111,175 KiB. These are main-process snapshots on one device, not a usage-feature memory attribution or endurance benchmark. A final dialog-copy reduction followed these samples. Existing animation loops and texture caches are unchanged. Full reboot, the complete 60-minute sleep interval and phone hardware were not retested.

## 0.8.1 usage footer and recorded tokens

- Java/Python checks cover token bounds, local-day expiry, independent unavailable quota/token states, cumulative deltas, first/fork observations, repeated records, counter resets, Claude streaming-message deduplication, cached-input arithmetic, missing log roots and Cincinnati's 23-hour DST day. Existing project checks and public/private Android builds passed.
- Installed with the existing signing identity. Actual Claude status-line data now supplies both short and weekly allowances; the prior authenticated-Claude observation gap is resolved. Both providers' local recorded token totals reached Home and the fullscreen card. No model request or fixture account data was used. A full Mac collect/read/publish cycle took about two seconds in this one check, including reading the local usage metadata; this is not a sustained collector benchmark.
- Inspected actual TV captures: Home retains its compact meters and adds token totals; fullscreen has one horizontal coding footer below the unchanged-size news, events and snow cards. These three cards move upward together to make room, leaving the clock's lower landscape open. Token totals explicitly say Mac today and include cached input; they are not account-wide billing figures. Native scene loops, texture caches and Home/bridge behavior are unchanged.

## 0.9.0 scrolling Home and separate screensaver cards

- Pure navigation checks cover the scroll-through tile route, reaching the wide final Watching tile from either column, both coding cards and weather, returning to prior grid focus, revealing the first app row, scrolled touch mapping, rejecting gutters and reachability of every tile/reading/footer control. Existing project checks and public/private Android builds pass.
- Installed in place and inspected the initial, middle and final tile rows on the TV. With ADB-generated D-pad input, opened All apps, Inputs, Settings, Ambient, Watching, each coding card and Cincinnati forecast; native panels were confirmed from their actual UI. A swipe through the grid followed by a tap on the right side of the wide Watching tile opened the correct panel. Back revealed the previously selected app; the initial app row and normal idle timing were restored. These checks are not renewed owner confirmation of physical-remote behavior.
- Home shows larger side-by-side provider cards, a current-condition glyph and temperature, and the first reported upcoming daytime high/nighttime low from the NWS forecast. Missing ranges stay unavailable; these next high/low values are not relabeled as an observed daily extreme. Actual cached observation and forecast data populated the panel. Icons use native primitives; no downloads or bitmap cache were added.
- Inspected the fullscreen screensaver with two equal provider cards beneath news/events/snow. Each provider's one or two reported windows share its available width, removing the shared-bar gap. Existing missing/saved/reset behavior, scene animation, system settings and Home bridge are unchanged. No new performance, full-reboot, or 60-minute sleep claim is made for this layout revision.

## 0.9.1 snow outlook beneath weather

- Public/private Android builds pass and the update installed in place. Configured Restore default resorts on the actual TV; the native locations panel showed exactly Perfect North and Timberline, WV, with Breckenridge and Alyeska absent. The owner explicitly selected Timberline Mountain, West Virginia.
- Inspected a fullscreen TV capture after both NWS grid forecasts loaded. Each location showed a separately timestamped, complete 48-hour snowfall horizon in this sample. The large text-only values sit beneath Cincinnati weather; the old Snow Forecast card is absent, and news/events occupy the two right-hand card positions. Coding cards remain below that row.
- The snowfall aggregation/cache policy is unchanged. Missing coverage still shows unavailable, older data retains saved labels, and configured points remain editable. A temporary Activity idle preview was cleared with a plain Home intent after visual inspection. This release does not claim new performance, physical-remote, full-reboot or 60-minute sleep validation.

## 0.9.2 equal-width screensaver meters

- Public/private Android builds passed and the update installed in place. Inspected the real fullscreen TV capture with the actual Codex weekly and Claude short/weekly readings: three 356-pixel cards, each with a 308-pixel bar in the 1920-pixel reference layout. The group spans x684–1824, exactly the outer edges of the news/events row above, with 36-pixel gutters.
- Window counts remain data-driven, missing providers retain an unavailable card, and long labels are bounded to the available column. Home layout, source observations, clock/weather/snow, scene motion and idle settings are unchanged. The temporary Activity idle preview was cleared after inspection; this is a layout check, not new performance or physical-remote evidence.
