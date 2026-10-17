# Architecture and decisions

## Native rendering

The first dashboard used an Android WebView with layered HTML/CSS conservatory art. On the target TV it continuously rendered even while idle and produced poor frame timings. The maintained implementation is an Android Activity with a custom Canvas HomeView and a separate PixelSceneView clipped to the open-window aperture.

HomeView draws the room, app rail, open-window reveal, sill, linen edge, and bottom console into Android's retained rendering pipeline. The opening has no sash or crossing dividers. App banners are loaded once from PackageManager into six 256x144 bitmaps. The contained window remains contained in Ambient mode.

PixelSceneView keeps a 480x336 terrain texture, separate tree/cloud sprites, a small Halloween layer, and two tiny code-drawn ghost sprites. Total retained decoded scene bitmaps are 2,757,040 bytes. Tree/cloud sprites are first enlarged with nearest-neighbor scaling; filtered subpixel movement then avoids coarse position jumps. Rooted tree meshes deform continuously across a fine grid; cloud positions are floating point. Water reuses the terrain texture through a clipped mesh plus procedural ripples. This animates selected foreground layers, not every distant tree or physical fluid.

Choreographer schedules approximately every other 60 Hz vsync (~30 fps). Only the scene View invalidates continuously. Activity pause removes the frame callback; no WebView, frame-sequence cache, video player, or per-frame bitmap decoding is used. The user confirmed the motion felt smoother after removing pixel snapping and increasing pacing from 12 fps.

CincinnatiSky uses offline approximate NOAA solar equations at Cincinnati city-center coordinates, cached once per minute. UTC calculations and the America/New_York calendar handle daylight saving time; both clock/date and October decoration boundaries use that zone. Sky color and terrain lighting blend with solar elevation. Stars and the crescent moon are decorative, not an accurate lunar ephemeris. Correct scene timing depends on the device clock. Pumpkins, bats, and a ghost with continuous roaming/bobbing motion are October-only. See [assets and lighting details](pixel-autumn/README.md).

## App and system integration

Favorites resolve Leanback launch intents, falling back to standard launch intents. The all-apps list queries launchable installed activities. The input picker uses TvInputManager and passthrough intents. Settings launches the known working Fire TV MainSettingsActivity directly. These actions depend on the target firmware and installed apps.

NWS weather uses HTTPS with connection/read timeouts, a 30-minute preference cache, a five-minute retry cooldown, and a stale marker after an hour. Forecast period temperature is not represented as a live sensor reading. Cincinnati coordinates are currently a source-level example default.

The optional Watch Next reader queries the standard Android TV provider and constrains resume intents to the publishing package. No rows were available on the tested TV. Reading Amazon's private provider was denied; the project does not bypass those permissions or claim universal viewing-history access.

## Home interception

The initial Home on Fire Accessibility Service configuration detects Amazon's launcher becoming foreground and then brings Sol forward. That inherently allows Amazon's Home screen to flash. Setting Sol as HOME returned Success at the package command but did not change the resolver result.

The direct bridge instead runs under an authorized ADB shell, grabs evdev input, and forwards the real mapped buttons into uinput. It handles Home before Android dispatches it, and starts Sol directly when Home is released. The binary is a small ARM32 static executable assembled with Clang and packaged by a narrow ELF packer. The kernel is ARM64 with compatibility support; Android userspace on the tested TV is 32-bit.

The supervisor is a shell process, not an Android boot service. It remains local while the TV session runs. An ordinary app's local ADB bootstrap was closed by Fire OS in the test, so full reboot persistence remains the main unresolved systems problem. The project has not rooted or unlocked the TV.
