# Architecture and decisions

## Native rendering

The first dashboard used an Android WebView with layered HTML/CSS conservatory art. On the target TV it continuously rendered even while idle and produced poor frame timings. The maintained implementation is an Android Activity with a custom Canvas HomeView and a separate transparent SceneMotion View.

HomeView draws the static interface and background into Android's retained rendering pipeline. App banners are loaded once from PackageManager into six 256x144 bitmaps. The landscape is resized to 1536x864 and retained in RGB565. SceneMotion caches a 640x160 ARGB mist texture (~400 KiB), draws it at changing positions, and draws at most five small leaf paths during intermittent gust periods. It invalidates at approximately 12 fps while the Activity is resumed and removes its callback on pause. No WebView, frame-sequence cache, video player, or continuous bitmap decoding is used in the maintained dashboard.

The animation currently suggests weather and wind; it does not deform the trees or animate the baked-in photographic clouds. The image prompts and preserved old implementation make the design evolution inspectable.

## App and system integration

Favorites resolve Leanback launch intents, falling back to standard launch intents. The all-apps list queries launchable installed activities. The input picker uses TvInputManager and passthrough intents. Settings launches the known working Fire TV MainSettingsActivity directly. These actions depend on the target firmware and installed apps.

NWS weather uses HTTPS with connection/read timeouts, a 30-minute preference cache, a five-minute retry cooldown, and a stale marker after an hour. Forecast period temperature is not represented as a live sensor reading. Cincinnati coordinates are currently a source-level example default.

The optional Watch Next reader queries the standard Android TV provider and constrains resume intents to the publishing package. No rows were available on the tested TV. Reading Amazon's private provider was denied; the project does not bypass those permissions or claim universal viewing-history access.

## Home interception

The initial Home on Fire Accessibility Service configuration detects Amazon's launcher becoming foreground and then brings Sol forward. That inherently allows Amazon's Home screen to flash. Setting Sol as HOME returned Success at the package command but did not change the resolver result.

The direct bridge instead runs under an authorized ADB shell, grabs evdev input, and forwards the real mapped buttons into uinput. It handles Home before Android dispatches it, and starts Sol directly when Home is released. The binary is a small ARM32 static executable assembled with Clang and packaged by a narrow ELF packer. The kernel is ARM64 with compatibility support; Android userspace on the tested TV is 32-bit.

The supervisor is a shell process, not an Android boot service. It remains local while the TV session runs. An ordinary app's local ADB bootstrap was closed by Fire OS in the test, so full reboot persistence remains the main unresolved systems problem. The project has not rooted or unlocked the TV.
