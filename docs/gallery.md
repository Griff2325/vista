# Vista screenshot gallery

Current TV captures from Vista 0.11.1, taken October 19, 2026, plus an earlier native iPhone simulator capture. These show the implemented interface; the [original design concepts](design-concepts/2026-10-17/README.md) are separate.

Scene images use temporary date/lighting previews or the built-in occasion picker. The TV clock and Cincinnati weather remain live. Window-only crops make the seasonal differences easier to compare. Home and ambient captures omit the private coding-usage footer; the Spotify image is cropped to its popout. No usage values, artwork or interface elements have been substituted.

## Home and ambient

![Vista Home with app-logo tiles and the autumn window at night](images/gallery/home.png)

**Home:** a scrollable two-column app grid beside one open landscape window. Explore, Prime Video and the familiar streaming shortcuts sit in the same remote-navigable grid.

![The lower Home grid with Spotify vibes and TV utility tiles](images/gallery/home-controls.png)

**Further down the grid:** Spotify vibes, All apps, Inputs, Ambient, Settings and Watching. News briefs and This week stay anchored beneath the scrollable tiles.

![Expanded autumn scene with clock, weather, snow outlook, news and Cincinnati events](images/gallery/ambient.png)

**Fullscreen ambient preview:** the window expands into the scene after idle, with a Cincinnati clock, winter countdown, forecast, Perfect North and Timberline Mountain snow outlooks, a publisher news brief and a next-seven-days Cincinnati pick. This capture uses the shortened preview timer; normal idle is two minutes. Coding allowance cards sit beneath the visible crop. Weather and feed contents are time-specific observations, not sample data.

## Explore

![Explore with a native sidebar, featured title, ratings and artwork grid](images/gallery/explore.png)

**Cross-service discovery:** native browsing with featured artwork, a continuously loading recycled title grid, title search and removable service/genre/year/rating filters. Catalog access and optional IMDb/Rotten Tomatoes ratings require locally configured API credentials. This is catalog discovery, not aggregated subscription accounts or private viewing history. [Setup and limits](media.md).

## Spotify vibes and phone companion

| Spotify vibes | Native iPhone companion |
| --- | --- |
| ![Spotify popout with six vibe shortcuts and transport controls](images/gallery/spotify-vibes.png) | ![Native SwiftUI companion Scene tab in the iPhone simulator](images/gallery/iphone-companion.png) |
| Playlist shortcuts and optional local media-session controls. No Spotify session was active in this capture, so transport is disabled. | Earlier simulator capture of the Scene tab, with seasonal previews, wildlife controls and ambient launch. Remote and Find tabs share the paired local connection. |

[Spotify behavior](media.md#spotify-vibes) · [iPhone build and pairing](../companion/ios/README.md).

## Four seasons

The four palettes share the same landscape geometry and native animation system. Winter artwork does not imply current snowfall: precipitation, clouds and wind still follow Cincinnati weather. The captures show individual frames; butterflies, fireflies, wildlife and other intermittent motion depend on daylight, weather and animation phase.

| Spring preview | Summer preview |
| --- | --- |
| ![Spring landscape with fresh green foliage and near-bank flowers](images/gallery/spring.png) | ![Summer landscape with deeper green foliage](images/gallery/summer.png) |
| Fresh green foliage, near-bank flowers and drifting petals; butterflies in calm daylight. | Deeper green foliage, daytime butterflies and sparse nighttime fireflies when weather permits. |

| Autumn preview | Winter preview |
| --- | --- |
| ![Orange and gold autumn forest around the lake](images/gallery/autumn.png) | ![Snow-colored winter forest with a turquoise and orange snowboard on the bank](images/gallery/winter.png) |
| Orange/gold foliage and falling leaves. | Snow-colored foliage/reflections, with a turquoise and orange snowboard propped on the dry foreground bank. |

## Holidays and celebrations

Accents are deliberately small additions to the scene. Automatic selection uses Cincinnati's calendar; Settings also offers explicit previews. Automatic fireworks appear at dusk/night with quiet periods between bursts. [Dates, controls and rendering details](seasons.md).

| Halloween preview | Thanksgiving preview |
| --- | --- |
| ![Halloween ghosts, pumpkins, cat and spider web in the autumn scene](images/gallery/halloween.png) | ![Harvest basket and gourds beside the autumn lake](images/gallery/harvest.png) |
| Ghosts, pumpkins, cat and web/spider. The witch with sparkling trail and bats appear intermittently. | A harvest basket and gourds on the near bank. |

| Christmas preview | New Year preview |
| --- | --- |
| ![Winter scene with a small string of colored lights and gifts](images/gallery/christmas.png) | ![Small nighttime fireworks over the winter lake](images/gallery/new-year.png) |
| Colored tree lights and wrapped gifts. | Small staggered fireworks over the winter landscape. |

| Valentine’s Day preview | St. Patrick’s Day preview |
| --- | --- |
| ![Winter lake with faint drifting pink hearts](images/gallery/valentine.png) | ![St. Patrick's Day scene with clover on the near bank](images/gallery/st-patrick.png) |
| Three gently drifting, fading hearts over the lake. | Clover on the near bank; the explicit holiday preset selects spring. |

| Earth Day preview | July Fourth preview |
| --- | --- |
| ![Spring Earth Day scene with small butterflies](images/gallery/earth-day.png) | ![Small nighttime fireworks over the summer lake](images/gallery/july-fourth.png) |
| Spring foliage and small butterflies, with evening fireflies when weather permits. | Restrained fireworks over the summer landscape. |

![Celebration preset with small fireworks over the lake](images/gallery/celebration.png)

**Celebration preview:** the same fireworks effect used for private annual events. The preset retains the selected season and also permits daylight fireworks for inspection. No personal event names or dates are included here.

## Scene controls

![Vista Seasons and occasions settings](images/gallery/season-controls.png)

Season selection, holiday accents, occasion previews and the annual-event calendar are available from **Settings → Seasons & occasions**. Saved preview overrides persist until **Restore automatic calendar** is selected. Automatic selection was restored after these captures.

These images document appearance. They do not establish animation smoothness, physical-remote behavior, playback compatibility or reboot persistence; see [device validation](validation.md) for those checks. Explore artwork and service logos belong to their respective owners; see [acknowledgments](../THIRD_PARTY_NOTICES.md).
