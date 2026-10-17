# Explore and Spotify

## Explore

The Home Explore tile opens a native movie/TV discovery screen. Installed-app shortcuts work immediately. Unified catalog search/discovery requires a personal TMDB read-access token or v3 API key, entered in **Catalog settings** on the TV. Obtain it from [TMDB API settings](https://www.themoviedb.org/settings/api); do not put it in source, screenshots or issue reports. The masked value is stored in private app preferences, sent over HTTPS to TMDB, and removed by Disconnect. Android backup is disabled.

Features include title search, movie/TV type, Netflix/Hulu/Disney+/Apple TV+ preferences, genre interests, popularity/rating/recent-release sorts, provider availability details and a local saved list capped at 100. US subscription/free/ad-supported offers come from TMDB/JustWatch; rental/purchase offers are excluded. Recommendations reflect selected interests and catalog sorting. No account sign-in, subscription entitlement or private viewing history is inferred.

Search checks at most twenty title matches per catalog page; filters can leave sparse pages. Availability uses at most four concurrent requests, a bounded 120-title in-memory cache and timeouts. No poster/video cache is downloaded. YouTube remains an app/search shortcut outside this catalog. Provider actions try supported app search/open intents; TMDB's provider data does not supply universal title-specific playback deep links.

No authorized live catalog query has been validated without an owner-supplied credential. Build/request-policy tests and no-credential TV checks do not establish streaming account aggregation.

## Spotify vibes

The separate Home tile opens a compact native panel. The regular Spotify logo tile still launches Spotify directly. Six default vibes open verified public Spotify playlists: Deep Focus, Chill Hits, Beast Mode, Jazz Vibes, Afternoon Acoustic and Sleep. **Edit vibes** can assign a verified Spotify playlist, album, artist or track link/URI. Links stay in private preferences; opening a link does not guarantee autoplay or exact TV-app navigation.

Optional playback controls use Spotify's actual local Android media session. Android requires notification-listener access, a broader permission than music control. The panel explains this before opening the access setting. The implementation ignores notification content, reads only Spotify title/artist/state, stores no listening history, downloads no artwork, and registers callbacks only while the panel is open. Controls are disabled unless the session advertises them. Another device's Spotify session is not automatically a session on this TV.

Some Fire OS versions omit the standard notification-access UI. After explicit owner consent, an authorized ADB shell can enable only Vista's component using the supported Android notification command:

```sh
adb -s YOUR_ADB_SERIAL shell cmd notification allow_listener com.griffin.solhome/.SpotifyMediaListener
```

Revoke it using `disallow_listener` with the same component. Do not enable this silently during installation. No Spotify OAuth credential, API/Connect integration or privileged media-control permission is bundled.

Default playlist targets were checked against Spotify's public oEmbed metadata. A generic “Chill” search resolved to a particular song in the TV test, so the shipped defaults use explicit playlist IDs instead. [Deep Focus](https://open.spotify.com/playlist/37i9dQZF1DWZeKCadgRdKQ), [Chill Hits](https://open.spotify.com/playlist/37i9dQZF1DX4WYpdgoIcn6), [Beast Mode](https://open.spotify.com/playlist/37i9dQZF1DX76Wlfdnj7AP), [Jazz Vibes](https://open.spotify.com/playlist/37i9dQZF1DX0SM0LYsmbMT), [Afternoon Acoustic](https://open.spotify.com/playlist/37i9dQZF1DX4E3UdUs7fUx), [Sleep](https://open.spotify.com/playlist/37i9dQZF1DWZd79rJ6a7lp). Playlist contents can change; owners can replace the shortcuts locally.
