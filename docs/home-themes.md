# Home themes

Choose **Themes** beside the Home header to open the picker. Use **Up** from the apps to reach it with the remote. You can also open the picker from **Vista Settings → Home theme**. Choose Classic, Park Rat, First Chair, Porchlight, River City, or GTA VI. Vista saves the selection on the TV. Existing Park Rat selections carry forward from 0.12.0. All of the themed layouts offer **More** for shortcuts including Settings, Inputs, Ambient, and Fire TV Home.

## First Chair

The snowfall line shows National Weather Service grid estimates for Perfect North, Mad River Mountain, Paoli Peaks, Breckenridge, Park City, Stowe, and Mount Snow. Each forecast covers a representative point near the ski area. The rotation advances every eight seconds while First Chair is on screen.

Press **Up** from the app dock to focus the ticker and pause it. Press **Right** to reach **Next resort** or press **OK** on the rotation toggle to resume. **Down** returns to the dock tile you came from. Vista labels cached forecasts more than two hours old as saved. When a forecast has expired or the NWS has no complete estimate for the point, the ticker shows that the forecast is unavailable. The two editable snow locations in Settings still feed the ambient weather cards.

## Porchlight

Open **Spotify vibes** from the record crate or the More menu to enable local Spotify playback access. Play music in Spotify on the TV, then return to Porchlight while it is playing. Vista can show up to three tracks it observes playing while Porchlight is visible. These tracks stay in memory for this Home session and clear when you leave the theme, restart Vista, or revoke access. The regular Spotify tile opens Spotify. A sleeve with a track link supplied by the session tries that track in Spotify. Other sleeves open Spotify vibes.

## GTA VI artwork

For a local development build with the complete GTA VI theme, put both authorized JPEGs in the ignored `.local/home-art/` directory at the repository root, then run `bash scripts/build-app.sh`. Rockstar’s official screenshot gallery labels the background **Jason and Lucia 03**. [Rockstar gallery](https://www.rockstargames.com/VI/media/screenshots).

| File | Source | SHA-256 |
| --- | --- | --- |
| `gta-vi-jason-lucia.jpg` | [Jason and Lucia 03, 1920 × 1080](https://www.rockstargames.com/VI/_next/static/media/Jason_and_Lucia_03.15z-.c0tptrzs.jpg?akim=1&imdensity=1&imwidth=1920) | `30b98cac824253479d431318a09dd4deab4ad2e778a93f2792eedb9aa790f441` |
| `gta-vi-tuner.jpg` | [Trailer 2 at 0:29.85](https://www.rockstargames.com/VI/media/videos) | `dd470e35e58ff944e6949786cda2e7a8ae58f0d2d4a8197843328d5235165bb5` |

The regular development build packages the launcher layout and shows artwork setup guidance. Keep the Rockstar JPEGs and any APK containing them local.
