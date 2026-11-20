# Home themes

Open **Vista Settings → Home theme** to choose Classic, Park Rat, First Chair, Porchlight, River City, or GTA VI. Vista saves the selection on the TV. Existing Park Rat selections carry forward from 0.12.0. All of the themed layouts offer **More** for shortcuts including Settings, Inputs, Ambient, and Fire TV Home.

## First Chair

The snowfall line shows National Weather Service grid estimates for Perfect North, Mad River Mountain, Paoli Peaks, Breckenridge, Park City, Stowe, and Mount Snow. Each forecast covers a representative point near the ski area. The rotation advances every eight seconds while First Chair is on screen.

Press **Up** from the app dock to focus the ticker and pause it. Press **Right** to reach **Next resort** or press **OK** on the rotation toggle to resume. **Down** returns to the dock tile you came from. Vista labels cached forecasts more than two hours old as saved. When a forecast has expired or the NWS has no complete estimate for the point, the ticker shows that the forecast is unavailable. The two editable snow locations in Settings still feed the ambient weather cards.

## Porchlight

Open **Spotify vibes** from the record crate or the More menu to enable local Spotify playback access. Play music in Spotify on the TV, then return to Porchlight while it is playing. Vista can show up to three tracks it observes playing while Porchlight is visible. These tracks stay in memory for this Home session and clear when you leave the theme, restart Vista, or revoke access. The regular Spotify tile opens Spotify. A sleeve with a track link supplied by the session tries that track in Spotify. Other sleeves open Spotify vibes.

## GTA VI artwork

The art comes from [Rockstar Games' official Grand Theft Auto VI trailers](https://www.rockstargames.com/VI/media/videos). For a local development build with the theme’s complete artwork, put both authorized JPEGs in the ignored `.local/home-art/` directory at the repository root, then run `bash scripts/build-app.sh`.

| File | Source frame | Approved preview SHA-256 |
| --- | --- | --- |
| `gta-vi-rooftop.jpg` | Trailer 1 at 0:38.52 | `5e4f22e98d8aedc0f6fb73a956e823ad376a9f2245f3d3b17419ffe383270226` |
| `gta-vi-tuner.jpg` | Trailer 2 at 0:29.85 | `dd470e35e58ff944e6949786cda2e7a8ae58f0d2d4a8197843328d5235165bb5` |

The regular development build packages the launcher layout and shows artwork setup guidance. Keep the trailer JPEGs and any APK containing them local.
