# Seasons, occasions and annual events

Open **Settings → Seasons & occasions** from Home. Automatic selection uses Cincinnati's `America/New_York` date, shared with the system screensaver. No online calendar, account, location permission or new background service is required. The existing Cincinnati events/news feeds remain separate live-data features.

| Scene | Fixed start | Appearance and motion |
| --- | --- | --- |
| Spring | March 20 | Fresh green foliage, near-bank flowers, drifting petals and two daytime butterflies |
| Summer | June 21 | Deeper green foliage, daytime butterflies and sparse nighttime fireflies |
| Autumn | September 22 | Original approved orange/gold artwork and falling leaves |
| Winter | December 21 | Snow-colored foliage/reflections and a turquoise/orange snowboard with bindings propped on the dry foreground bank |

These dates define the visual schedule; they are not calculated annual equinox/solstice instants. Winter continues through March 19. The existing December 21 winter countdown stays visible in the screensaver. Snowy winter artwork does not imply current snowfall: animated rain/snow, wind, cloud cover and haze continue to use the Cincinnati weather source, including its stale-data behavior.

## Occasion windows

| Occasion | Cincinnati dates | Accents |
| --- | --- | --- |
| Halloween | October 1–31 | Original three ghosts, witch/sparkles, web/spider, pumpkins, cat and occasional bats |
| Thanksgiving | Four days before the fourth Thursday of November through the following Sunday | Near-bank harvest basket/gourds, without Halloween faces or characters |
| Christmas | December 1–30 | Small colored tree lights and gifts; winter palette follows its own season date |
| New Year | December 31–January 2 | Small staggered fireworks at dusk/night |
| Valentine’s Day | February 12–14 | Three slowly drifting, fading hearts |
| St. Patrick’s Day | March 16–17 | Near-bank clover |
| Earth Day | April 20–22 | Butterflies and evening fireflies |
| July Fourth | July 2–4 | Small staggered fireworks at dusk/night |
| Personal annual event | Its saved month/day | Celebration fireworks at dusk/night and the event name in the scene status |

There are quiet periods between fireworks; they never brighten the whole screen. Manual celebration previews also show them during daylight so the effect can be inspected. Seasonal motion and the live local-events feed do not depend on the holiday-accents toggle. The toggle suppresses automatic holiday and personal-celebration effects; explicitly selecting a holiday preview is an override.

## Preview and calendar controls

- **Season** selects Automatic or one of the four scenes. Selecting a season previews it without holiday decorations. Overrides survive app restarts until restored; both Home and Dream use them.
- **Preview a holiday or celebration** pairs a holiday with a suitable season. No clock, weather observation, news item or real event date is changed. Home and fullscreen label overrides as **Preview**.
- **Restore automatic calendar** clears both preview overrides, preserving the accents on/off preference and personal events.
- **Calendar & annual events** lists upcoming built-in dates and personal events, with explicit dates and days remaining. Add a title of up to 40 characters and an `MM-DD` date; up to 20 events are stored locally in app-private preferences. Select a saved event in Manage to remove it. February 29 events retain their actual leap-year date, including across non-leap centuries. A personal event on the current date takes precedence over automatic holiday decor.

## Rendering and limits

The same generated landscape geometry is used across seasons. `SeasonPalette` maps warm foliage/reflection pixels while loading the low-resolution source, preserving transparency, blue water, evergreen shade, gray rocks and dark bark. Spring/summer mapping feathers into the wooded hills so sunlit mountain rock does not turn green. It then uses the existing nearest-upscaled textures. This is a palette system with native accents, not four independent illustrated landscapes or a leafless-tree simulation. Butterflies/fireflies are suppressed during rain, snow or strong wind.

The cache lazily retains at most four read-only seasonal texture sets, one per season. Home and Dream reuse those same bitmap identities on subsequent visits, avoiding retired CPU/GPU images accumulating with each preview. Clouds, Halloween assets and small sprites are shared across every set. One active season uses 8,202,040 decoded bytes (~7.82 MiB); after all four have been visited, the bounded decoded cache is 30,444,856 bytes (~29.03 MiB). Process memory also includes GPU caches, UI/runtime resources and temporary decode buffers; decoded bytes are not total RAM use.

First-time decoding/recoloring runs on a worker. Paused/superseded requests cannot replace a view's current selection. No image sequence, video or per-frame recoloring is added. Snowboard, flowers, lights, holiday shapes and fireworks use fixed-size native drawing loops and reusable paint/geometry. Ripple curves use eight precomputed line segments per arc and a fixed draw buffer; animated Canvas arcs were avoided after device memory checks.

`scripts/test-sky.sh` covers date boundaries, holiday windows, moving Thanksgiving dates, upcoming ordering, leap-day recurrence, Cincinnati midnight/DST, palette preservation and the existing scene/data policies. Physical rendering checks are recorded in [validation](validation.md).
