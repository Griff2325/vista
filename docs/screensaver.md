# Screensaver, public data and recovery

Vista expands the Home window over two seconds, completing at two minutes without input. The first navigation button returns to Home without also opening the selected app. Volume/power continue to their normal handlers. A noninteractive Android DreamService provides the same scene when the system owns idle behavior; playback apps can keep the TV awake.

## Device-specific setup

The tested Fire OS firmware overrides ordinary Android screensaver preferences with Amazon Ambient. Install Vista 0.5 first, then use an already authorized ADB connection:

```sh
python3 scripts/configure-screensaver.py --serial YOUR_ADB_SERIAL
```

Add `--adb /path/to/adb` if it is not on PATH. The script saves the original known values in ignored `.local/screensaver/`, disables the Amazon Ambient selector, selects Vista's DreamService, sets `screen_off_timeout=120000` and `sleep_timeout=3600000`, and verifies readback. A shell-only Java entry point discovers the framework's `setDreamComponents` transaction and selects the legacy Android dream route. No ordinary-app privileged dream permission is granted.

A local shell supervisor re-arms that transient route every twenty seconds. The laptop does not render frames or participate in that loop. The supervisor uses a PID file and is restarted by the setup script. **It must be restarted after a full TV reboot.** The Home input bridge has the same unresolved bootstrap limitation. Do not represent this as a rooted or complete boot replacement.

To restore the saved screensaver values and stop Vista's dream supervisor:

```sh
python3 scripts/configure-screensaver.py --serial YOUR_ADB_SERIAL --restore
```

Preserve the local recovery snapshot. This does not uninstall Vista, remove the Home bridge, disable core packages or reset the TV. Changing Fire TV settings later can change the configuration. A 60-minute setting readback is not proof of a complete observed sleep cycle.

## Cards and sources

| Card | Data and refresh |
|---|---|
| Cincinnati | NWS city-center forecast and latest KLUK observation, 10 minutes; observed temperature, available feels-like/humidity/wind, precipitation chance and next three forecast periods; scene uses fresh observation description/wind |
| News | MIT News AI research and BBC World RSS, 30 minutes; limited publisher briefs, not generated headline guesses |
| This week | Washington Park/Fountain Square event instances, Memorial Hall show dates, ESPN Bengals/FC Cincinnati home games, MLB Reds home games, and WCPO food RSS, 6 hours |
| Snow outlook below weather | NWS point-to-grid snowfall amounts for two locations, hourly; summed/prorated over the next 24–72 hours |

The weather/snow text, two news/events cards and the coding allowance cards redraw on data arrival or a 15-second clock, separately from the landscape. News and events occupy the two right-hand positions in their row; the former standalone Snow Forecast card is removed. AI/world briefs alternate every 45 seconds. Event picks rotate through one combined list. Each snow point shows its covered horizon (72 hours, falling back to 48 or 24 hours when the grid does not cover the longer window).

“This week” means today and the next six Cincinnati calendar dates, including currently ongoing events. Finished events disappear when an end time is known; a date-only show is labeled “time not listed.” Ordinary recurring bar/porch opening hours are excluded. Interest filters live on the TV under This week or Settings. Recent food reporting is labeled with its publication date, not treated as a scheduled opening or an event. Coverage is limited to the named sources, and all times/tickets should be confirmed with the organizer.

Default snow points: Perfect North and [Timberline Mountain in Davis, West Virginia](https://timberlinemountain.com/contact/). Settings → Snow locations can edit names/US coordinates, replace either of the two displayed locations, remove points or restore defaults. Existing saved lists are preserved on update; Restore default resorts adopts the new pair. The TV used for this update was configured with those two points. These are grid estimates near the chosen coordinates, not resort snow stakes, base depths or guaranteed mountain totals. Missing grid coverage/unsupported units show unavailable rather than zero.

Cached data survives loss of connection and is marked saved/stale. Requests use two bounded workers, response caps, HTTPS and timeouts. No streaming or news account credentials are used. Correct calendar/solar behavior depends on the TV clock.

## Winter counter

The clock includes a Cincinnati-local calendar countdown to December 21, showing “Winter is here” during the Dec 21–Mar 19 winter period. December 21 is a fixed calendar target, not an annual astronomical ephemeris; the [US Naval Observatory 2026 seasons data](https://aa.usno.navy.mil/api/seasons?year=2026) confirms the 2026 December solstice falls on that date locally. The separate scene calendar now switches the winter palette on the same fixed December 21 date; manual previews can override the artwork without changing this clock/countdown. [Seasons & occasions](seasons.md) describes the schedule and controls.

Observation fields that NWS does not supply remain omitted/unavailable. The precipitation percentage and upcoming temperatures are forecast values, shown separately from the timestamped current observation. The scene and widgets share the same cache; more detailed weather adds no new per-frame network requests.
