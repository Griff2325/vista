# Seasonal landscape — open-window scene

The owner selected Pixel Autumn from the [four illustrated concepts](../design-concepts/2026-10-17/README.md). The implemented dashboard treats the landscape as a view outdoors: one uninterrupted opening, deep timber reveal/sill, a smooth recessed app rail with no row dividers, and a shallow console for utilities and forecast. The surrounding surfaces use smooth, simple native Canvas drawing, retained separately from scene motion. No screenshot from the visual inspiration is bundled.

![Installed open-window dashboard](../images/dashboard.png)

![Night lighting on the installed TV](../images/pixel-night.png)

## Artwork and generation

Generated with the **built-in image_gen tool**, using the approved Pixel Autumn concept as a palette/style reference. Exact asset prompts and transparency requirements are in [asset-prompts.json](asset-prompts.json). Assets are original generated illustrations; installed application logos are loaded at runtime.

| Asset | Runtime purpose |
|---|---|
| [Terrain](../../app/assets/pixel-autumn/terrain.png) | 480x336 cached mountains, distant forest, lake, and banks; transparent sky |
| [Left tree](../../app/assets/pixel-autumn/tree-left.png) | Rooted mesh sway; 144x192 logical pixels |
| [Right tree](../../app/assets/pixel-autumn/tree-right.png) | Rooted mesh sway; 112x152 logical pixels |
| [Cloud](../../app/assets/pixel-autumn/cloud.png) | 192x64 logical pixels reused for drifting clouds |
| [Halloween](../../app/assets/pixel-autumn/halloween.png) | 100x67 pumpkin cutout, October only |

Terrain and moving tree/cloud textures are nearest-upscaled 3x once, then sampled with filtering during fractional movement. The chunky design remains visible while travel avoids whole-pixel jumps. Including procedural cloud variants and Halloween sprites, one season’s retained scene bitmaps total **8,202,040 bytes (~7.82 MiB)**, shared by Home and the system dream. The lazy four-season cache is bounded at **30,444,856 bytes (~29.03 MiB)** after every season has been visited; common assets are shared. Source PNG dimensions/file sizes are not runtime decoded-memory costs.

## Motion and lifecycle

Choreographer paces scene invalidation at about 30 fps on the tested 60 Hz TV. Rooted mesh deformation gives trees continuous sway and mild intermittent gusts. Clouds translate smoothly. Water stays fixed beneath shallow, broken elliptical ripple arcs that expand and fade inside a conservative lake mask. Home draws four ripple systems and fullscreen draws six, each with two rings; this uses a reusable paint/rectangle and no new textures or bitmap resampling. A shared bounded phase integrates wind speed, so weather updates and pause/resume do not accelerate the lake with device uptime. Both banks retain a dry margin. Leaf travel integrates wind speed over time, with fractional positions, gentle flutter and lifecycle fades; changing wind does not reset positions. A few leaves and nighttime bats add sparse motion. These are selected animated layers, not a complete forest or fluid simulation.

Three variations of the original ghost roam independently between deterministic random waypoints, bob and blink, and fade out/reappear on staggered schedules. They pass behind foreground trees. An occasional witch with a sparse sparkle trail, a larger hanging left-tree spider and separate smooth silk web, and a cat on the foreground rocks complete the Halloween accents. Automatic Halloween decorations are limited to October in Cincinnati; an explicit preview can override the date. [Seasons & occasions](../seasons.md) adds spring/summer/winter palettes, a winter snowboard, other holidays and a private annual calendar. Ambient mode smoothly expands to fullscreen and increases selected forest/water/seasonal effects. The Activity stops scene callbacks on pause; no video, decoded frame sequence, or per-frame image decoding is used.

## Cincinnati lighting

[CincinnatiSky.java](../../app/src/com/griffin/solhome/CincinnatiSky.java) calculates approximate solar elevation offline using the [NOAA equations](https://gml.noaa.gov/grad/solcalc/solareqns.PDF), at 39.1031 N, 84.5120 W. Sky, terrain, and cloud tint blend through day, dawn/dusk, and night. The moon/stars are decorative; lunar phase is not calculated. Lighting follows the TV timestamp; local dates, clock, and October boundaries use America/New_York. Solar timing is offline. Current cloud cover, wind, haze and bounded rain/snow effects additionally follow the shared NWS Cincinnati observation cache; stale observations suppress active precipitation.

`bash scripts/test-sky.sh` checks seasonal elevation, midnight darkness, dawn/dusk, calendar/holiday boundaries, timezone independence, and DST continuity. Developer-only `sol.preview.epoch` intent extras preview scene dates without changing the system clock or preferences; a normal Home intent clears that temporary override. User-facing season/occasion previews are separately persisted in local settings and clearly labeled; Restore Automatic clears them.

[Validation and performance](../validation.md) records device measurements and their limits.
