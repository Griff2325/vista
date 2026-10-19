# Explore and Spotify

## Explore

The Home Explore tile opens a native, Apple TV-inspired browsing screen: dark sidebar, a featured backdrop, horizontal movie/series shelves, visible D-pad focus, a local My list and installed-app banners. It keeps Android rendering and does not embed a web player or autoplay video.

**Free setup:** open Explore → Settings and enter a personal [TMDB read-access token or v3 API key](https://www.themoviedb.org/settings/api). TMDB requires its own account/API registration for personal non-commercial use. Vista does not create accounts, enroll in plans or purchase anything. The existing development TV had no catalog key configured when this refactor began; app shortcuts work without one. Keep keys out of source, screenshots and issue reports. Keys remain in private app preferences, sent only over HTTPS to their respective providers; Disconnect removes both. Android backup is disabled.

**Search across services:** title search spans TMDB movies and series and displays all reported US subscription, free and ad-supported offers, including providers outside the app-shortcut list. It retains titles with no offer or failed availability lookup and excludes people from multi-search. Service and genre preferences apply to browsing, not title search. All services is the default browsing mode. Selected browsing services include Netflix, Hulu, Disney+, Apple TV+, Prime Video, Max, Peacock, Paramount+, Tubi and Pluto; supported installed apps can be opened from title details. YouTube is an app/search shortcut outside this movie/series catalog. Search does not query each app's internal catalog, subscriptions or private viewing history. Rental/purchase offers are excluded; confirm access and subscription tier in the provider app. Provider actions try supported app search/open intents; TMDB does not provide universal title-specific playback links.

**Optional free ratings:** add an [OMDb free API key](https://www.omdbapi.com/apikey.aspx?at=freeAcct) in the same settings form. Its free account currently allows 1,000 requests per day; Vista does not require a paid plan. Ratings use the exact IMDb ID supplied by TMDB, not title guessing. IMDb and Rotten Tomatoes have separate labels on featured titles, cards and details. Missing/invalid scores show a dash, with the reason in details. TMDB votes are never passed off as IMDb or Rotten Tomatoes. Some titles, especially series, have no Rotten Tomatoes score in OMDb. A missing key, exhausted free quota or ratings outage does not block catalog results. Top rated browsing still sorts by clearly identified **TMDB votes**.

Work stays bounded: at most twenty titles per catalog page, four availability workers and one 35-second availability deadline after catalog requests. Sparse pages can still occur with selected-service browsing. Ratings load after cards appear using two workers, a bounded 120-title cache and 15-minute cache lifetime; navigating away from a query cancels its remaining rating work. Artwork uses TMDB's fixed HTTPS image host, two download workers, response/dimension limits, and an 8 MiB memory-only bitmap cache. No image cache is saved to disk. Saved titles remain capped at 100 and retain their last availability lookup timestamp.

Offline HTTP-fixture tests cover searches across services, provider failures, free key absence, exact IMDb IDs, rating validation, quotas and cancellation. On-device UI fixtures validate populated shelves without pretending they are live results. Live catalog/ratings validation remains pending an owner-supplied free key. See [validation](validation.md#explore-011) for the checks and limits.

## Spotify vibes

The separate Home tile opens a compact native panel. The regular Spotify logo tile still launches Spotify directly. Six default vibes open verified public Spotify playlists: Deep Focus, Chill Hits, Beast Mode, Jazz Vibes, Afternoon Acoustic and Sleep. **Edit vibes** can assign a verified Spotify playlist, album, artist or track link/URI. Links stay in private preferences; opening a link does not guarantee autoplay or exact TV-app navigation.

Optional playback controls use Spotify's actual local Android media session. Android requires notification-listener access, a broader permission than music control. The panel explains this before opening the access setting. The implementation ignores notification content, reads only Spotify title/artist/state, stores no listening history, downloads no artwork, and registers callbacks only while the panel is open. Controls are disabled unless the session advertises them. Another device's Spotify session is not automatically a session on this TV.

Some Fire OS versions omit the standard notification-access UI. After explicit owner consent, an authorized ADB shell can enable only Vista's component using the supported Android notification command:

```sh
adb -s YOUR_ADB_SERIAL shell cmd notification allow_listener com.griffin.solhome/.SpotifyMediaListener
```

Revoke it using `disallow_listener` with the same component. Do not enable this silently during installation. No Spotify OAuth credential, API/Connect integration or privileged media-control permission is bundled.

Default playlist targets were checked against Spotify's public oEmbed metadata. A generic “Chill” search resolved to a particular song in the TV test, so the shipped defaults use explicit playlist IDs instead. [Deep Focus](https://open.spotify.com/playlist/37i9dQZF1DWZeKCadgRdKQ), [Chill Hits](https://open.spotify.com/playlist/37i9dQZF1DX4WYpdgoIcn6), [Beast Mode](https://open.spotify.com/playlist/37i9dQZF1DX76Wlfdnj7AP), [Jazz Vibes](https://open.spotify.com/playlist/37i9dQZF1DX0SM0LYsmbMT), [Afternoon Acoustic](https://open.spotify.com/playlist/37i9dQZF1DX4E3UdUs7fUx), [Sleep](https://open.spotify.com/playlist/37i9dQZF1DWZd79rJ6a7lp). Playlist contents can change; owners can replace the shortcuts locally.
