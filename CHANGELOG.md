# Changelog

Repository established October 17, 2026. Earlier development stages below are supported by retained snapshots and session measurements; they were not originally Git commits. All imported history uses the actual import date.

## Development — October 18, 2026

- Separate private Hulu Lab experiment, with version-pinned on-demand ad-entry patch generation, a self-contained loopback player loader, redacted native diagnostics and boundary/state-transition tests. Patched player startup works without root; signed-in playback and actual ad removal remain unverified. No Vista runtime change or proprietary app artifact is included.

## 0.9.3 — October 18, 2026

- Optional Android 11+ Hulu ad muting, with explicit accessibility activation, local badge-only pixel matching and a separate service process. Preserves prior mute/volume choices, restores owned mutes after the badge disappears, and provides a settings off switch. Ad timing and protected playback remain unchanged.

## 0.9.2 — October 18, 2026

- Equal-width screensaver allowance cards: Codex weekly and Claude's short/weekly windows share the available width, aligned with the outer edges of the news/events row above. Missing providers keep an explicit unavailable state; other reported-window combinations adapt without reserving empty slots.

## 0.9.1 — October 18, 2026

- Moved snow forecasts into a text-only two-location outlook beneath the Cincinnati screensaver weather, matching its typography and large values. Removed the standalone Snow Forecast card and shifted news/events into the two right-hand card positions.
- Default snow locations are Perfect North and Timberline Mountain, West Virginia. Settings supports replacing the two displayed points; saved older lists can adopt the new pair through Restore default resorts. Amounts retain explicit coverage horizons, unavailable states and saved-data labels.

## 0.9.0 — October 18, 2026

- Scrollable native Home tile grid includes All apps, Inputs, Ambient, Settings and Watching. Remote focus reveals new rows with a short interruptible scroll; touch swipes and scrolled hit testing share the same viewport. News and This week stay anchored below the grid.
- Larger side-by-side Codex and Claude cards replace the former utility row. Single reported allowance windows fill their card; two-window providers split the space. Weather gains native condition glyphs, observed temperature when fresh, and the next reported daytime high/nighttime low.
- Screensaver coding usage now uses two balanced provider cards below news/events/snow, each adapting its bar layout to one or two reported windows.

## 0.8.1 — October 18, 2026

- Moved screensaver coding limits into a horizontal footer beneath the existing news, Cincinnati events and snow cards, clearing the landscape below the clock.
- Added separately labeled Mac-today token totals on Home, the screensaver and usage details. Local metadata aggregation includes cached input, deduplicates Codex cumulative/fork observations and Claude streaming blocks, and follows Cincinnati midnight/DST. Account allowance percentages remain independent; this is not a billing or cross-device token total.

## 0.8.0 — October 18, 2026

- Native Codex and Claude Code allowance meters in Home's utility console and beneath the screensaver clock, with a remote-accessible detail panel and Cincinnati-local reset times. Each provider's reported window duration determines its label; missing limits are never assumed to be zero.
- Optional per-user macOS collector reads Codex's account-limit RPC and Claude Code's documented status-line quota fields. Five-minute ADB sync sends only normalized percentages, window durations and timestamps. Credentials stay in the providers' existing Mac stores; no new TV listener or account API is introduced.
- Saved readings retain their source timestamp, show a saved marker after 15 minutes, and disappear after their reset or 24 hours. Claude readings require a normal Claude Code response after sign-in and status-line setup. No model request is made by the collector.

## 0.7.0 — October 17, 2026

- Lightweight wildlife sightings: a fox that pauses on the dry bank, passing birds and a winter snowboarder on the far ridge. Independent periods, daylight/weather gating, shared Home/Dream timing and a wildlife toggle. No new scene bitmaps or extra animation loop.
- Native SwiftUI iPhone companion project with camera QR scanning, manual address entry, memory-only credentials and local-network endpoint restrictions. Includes simulator build and client protocol tests; physical iPhone signing/install remains owner setup.
- Local TV companion service and optional browser fallback: TV QR plus expiring single-use code, one revocable browser session, visible opt-in foreground service, fixed commands, bounded HTTP transport and idle/absolute expiry. Phone UI provides Vista navigation, typed Explore searches, app shortcuts, scene selection and Spotify transport. Uses trusted-network HTTP; no cloud account or boot listener.
- Reports Fire OS background-launch restrictions explicitly: return from streaming apps and system dreams with the physical remote. Explore still needs its optional catalog credential; Spotify controls require an available session and existing notification access.

## 0.6.0 — October 17, 2026

- Four Cincinnati calendar seasons: spring blossoms/petals/butterflies, summer greens and nighttime fireflies, the original autumn colors/leaves, and snowy winter foliage with a foreground snowboard and visible bindings.
- Holiday windows for Halloween, Thanksgiving, Christmas, New Year, Valentine’s Day, St. Patrick’s Day, Earth Day and July Fourth. Sparse native accents include tree lights/gifts, harvest gourds, hearts, clover and intermittent small fireworks; Halloween characters stay confined to their occasion.
- Settings → Seasons & occasions provides persistent, clearly labeled season/holiday previews, an accents toggle, upcoming calendar dates and up to 20 private annual events. Personal events trigger a celebration on their Cincinnati date. Restore Automatic returns both Home and the system screensaver to live calendar selection.
- Palette conversion runs on the first visit to a season. A bounded lazy cache shares at most four read-only seasonal texture sets, avoiding repeated CPU/GPU image creation during previews, with no video, new raster assets or per-frame recoloring. Weather and clocks remain live during user previews; precipitation still follows Cincinnati weather.

- Ripple curves use precomputed short line segments instead of animated Canvas arcs after on-device A/B checks exposed native/GPU memory growth in the curved drawing path. Original rooted tree and distant-forest sway are preserved.

## 0.5.1 — October 17, 2026

- Fixed water phase acceleration during changing wind: integrate a bounded wave phase instead of multiplying wind by elapsed uptime. Home and the system dream share the same phase.
- Replaced lake texture deformation and sliding highlights with sparse, shallow expanding ripple arcs. The terrain and shoreline stay fixed. Four ripple systems on Home, six in fullscreen, two fading rings each; no new textures, resampling, or frame allocations.

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
