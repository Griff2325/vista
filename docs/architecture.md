# Architecture and decisions

## Native rendering

The first dashboard used an Android WebView with layered HTML/CSS conservatory art. On the target TV it continuously rendered even while idle and produced poor frame timings. The maintained implementation is an Android Activity with a custom Canvas HomeView and a separate PixelSceneView clipped to the open-window aperture.

HomeView draws the room, app rail, open-window reveal, sill, and bottom console into Android's retained rendering pipeline. The opening has no sash or crossing dividers. App banners are loaded once from PackageManager into one 256x144 bitmap per favorite (currently seven). At two minutes idle, a two-second ValueAnimator expands the aperture to the full screen while fading the retained room and introducing the widget cards.

PixelSceneView uses a 480x336 logical landscape, separate tree/cloud sprites, a small Halloween layer, and tiny code-drawn sprites. The terrain is nearest-upscaled 3x once as well as the moving sprites, so fullscreen sampling does not blur entire source pixels. Retained decoded scene textures total 8,202,040 bytes (~7.82 MiB), shared between Home and the DreamService. Tree/cloud sprites are first enlarged with nearest-neighbor scaling; filtered subpixel movement then avoids coarse position jumps. Rooted tree meshes deform continuously across a fine grid; cloud positions are floating point. Water remains stationary under clipped, expanding/fading ripple arcs driven by an integrated shared phase. Fullscreen mode adds gentle distant-forest deformation, more seasonal particles and additional ripple systems; summer/Earth Day add weather-gated nighttime fireflies. These are selected ambient effects rather than a physical forest/fluid simulation.

Choreographer schedules approximately every other 60 Hz vsync (~30 fps). Only the scene View invalidates continuously. Activity pause removes the frame callback; no WebView, frame-sequence cache, video player, or per-frame bitmap decoding is used. The user confirmed the motion felt smoother after removing pixel snapping and increasing pacing from 12 fps.

CincinnatiSky uses offline approximate NOAA solar equations at Cincinnati city-center coordinates, cached once per minute. UTC calculations and the America/New_York calendar handle daylight saving time; both clock/date and the scene calendar use that zone. Sky color and terrain lighting blend with solar elevation. Stars and the crescent moon are decorative, not an accurate lunar ephemeris. Correct scene timing depends on the device clock. Pumpkins, bats, three variations of the original ghost, a 24-second witch crossing with a bounded sparkle trail every 150 seconds, a larger hanging spider with a separate smooth silk web, and a blinking cat are automatic October accents, with explicit preview overrides. See [assets and lighting details](pixel-autumn/README.md).

SceneCalendar selects four seasonal palettes and occasion windows offline; SceneSettings stores preview overrides, an accents toggle and up to 20 private annual events. SeasonPalette recolors logical images on a worker. A lazy cache retains at most four read-only seasonal texture sets, reusing bitmap identities across preview switches rather than accumulating retired full-resolution images. One season uses ~7.82 MiB of decoded scene images; all four cached use ~29.03 MiB. Home and Dream share those textures and settings. SceneDecor draws the snowboard, flowers, lights and other bounded native accents. See [seasons and occasions](seasons.md).

## App and system integration

Favorites resolve Leanback launch intents, falling back to standard launch intents. The all-apps list queries launchable installed activities. The Inputs tile requests one native Input-key press through the existing shell-only control provider. The helper runs the fixed native bridge command `--inputs`; its temporary virtual remote emits scan code 641 without opening or grabbing the physical remote. The TV owns source selection and HDMI playback. Generic passthrough intents resolve to Live TV on the tested firmware and can open Channel Scan; direct native player/selector intents require system permissions. Settings launches the known working Fire TV MainSettingsActivity directly. These actions depend on the target firmware and installed apps.

WidgetRepository shares bounded public-data caches between Home and the system dream. Two IO workers use response caps, ten-second timeouts and five-minute retry cooldowns. Cincinnati forecast/observation refreshes every ten minutes; news every thirty minutes; local sources every six hours; snow every hour. Old values stay labeled as saved instead of appearing live.

The forecast period temperature is not a sensor reading. A fresh KLUK observation supplies the scene description and meteorological wind direction/speed. Cloud coverage, light, haze, tree gusts, leaves and water ease toward that observation. Rain/snow particles require a fresh reported observation; probabilistic or stale precipitation is not replayed as current weather. Five tiny cloud silhouettes are cached once, with at most eleven cloud sprites and fewer than 85 precipitation particles.

Publisher RSS descriptions supply news briefs; Vista does not invent summaries from headlines. The AI feed also requires an explicit AI/ML subject in its headline or publisher brief, so loosely tagged general MIT stories do not fill the AI card. Structured Washington Park/Fountain Square instances and Memorial Hall show dates merge with home-game schedules and dated WCPO food reporting. Local calendar boundaries, expiry and editable interest filters apply at display time. Food articles are labeled reporting, never assigned fictional event times. Snowfall grid intervals are converted to inches and summed/prorated over 24–72 hours; incomplete coverage is unavailable rather than zero. See [data sources and timing](screensaver.md).

Explore uses a separate, optional personal TMDB credential stored only in private app preferences. Spotify media-session callbacks exist while the popout is open, and after a control is requested during an explicitly enabled companion session. See [media setup and limitations](media.md).

The optional Watch Next reader queries the standard Android TV provider and constrains resume intents to the publishing package. No rows were available on the tested TV. Reading Amazon's private provider was denied; the project does not bypass those permissions or claim universal viewing-history access.

## Home interception

The initial Home on Fire Accessibility Service configuration detects Amazon's launcher becoming foreground and then brings Vista forward. That inherently allows Amazon's Home screen to flash. Setting Vista as HOME returned Success at the package command but did not change the resolver result.

The direct bridge instead runs under an authorized ADB shell, grabs evdev input, and forwards the real mapped buttons into uinput. It handles Home before Android dispatches it, and starts Vista directly when Home is released. The binary is a small ARM32 static executable assembled with Clang and packaged by a narrow ELF packer. The kernel is ARM64 with compatibility support; Android userspace on the tested TV is 32-bit.

The supervisor is a shell process, not an Android boot service. It remains local while the TV session runs. An ordinary app's local ADB bootstrap was closed by Fire OS in the test, so full reboot persistence remains the main unresolved systems problem. The project has not rooted or unlocked the TV.

## System dream routing

SolDreamService shares cached scene textures with Home, renders fullscreen, and obeys DreamService start/stop callbacks. It does not launch over an actively playing app itself. Fire OS otherwise selects its proprietary Ambient service regardless of the normal dream preference. The reversible shell setup disables that vendor selector and invokes the framework's shell-authorized setDreamComponents path. A small local shell supervisor re-arms the transient legacy route every twenty seconds. Transaction discovery uses the device framework rather than a hard-coded binder ordinal. This is firmware-specific, requires already authorized ADB shell, and does not create reboot persistence. No privileged permission is requested for the ordinary app.

## Companion and wildlife

[CompanionService](companion.md) is a user-started non-sticky foreground service with one bounded local HTTP worker. It accepts paired, fixed commands and marshals them onto the Android main thread; it does not inject into other apps. Its optional notification-access use remains Spotify-only. The physical Home bridge is unchanged.

[WorldOutside](world-outside.md) draws sparse bank/sky/ridge sightings using reusable native geometry and monotonic timing shared by Home and Dream. It uses the existing scene lifecycle and Cincinnati weather state.

## Optional Hulu muting

[HuluAdMuteService](hulu-ad-muting.md) runs in a separate accessibility process, enabled explicitly through Android. It checks the foreground package before sampling a tiny Ad-badge crop; no screenshots or text are retained or transmitted. A pure Java badge classifier and mute-ownership policy control idempotent Android audio mute operations, with manual overrides, capture timeouts and normal shutdown restoration. Home input interception and the scene lifecycle are unchanged.
