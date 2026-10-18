# Coding usage

Home's bottom console shows side-by-side allowance cards for Codex and Claude Code. Move right from a tile to select Codex, then right again for Claude and Cincinnati weather; left returns to the last selected tile. Open either coding card or Settings → Coding usage for all available windows, reset times and the age of each reading. The screensaver uses two balanced provider cards below the news, events and snow row. Each provider's reported windows fill its own card, without reserving an empty column for an unavailable window. Percentages mean **used**, not remaining. All reset times use America/New_York, the same Cincinnati timezone as the scene.

## Sources

The optional Mac helper uses two documented local interfaces:

- Codex [`account/rateLimits/read`](https://developers.openai.com/codex/app-server), through a short-lived `codex app-server --stdio` process using the existing CLI sign-in. It prefers the main `rateLimitsByLimitId.codex` allowance and falls back to `rateLimits`. Window duration determines whether a reading is labeled 5h, Weekly or another duration; primary/secondary ordering is not treated as a duration guarantee. Additional model-specific buckets are not combined.
- Claude Code's official [`statusLine` input](https://code.claude.com/docs/en/statusline), using only `rate_limits.five_hour` and `rate_limits.seven_day`. These fields depend on account eligibility and appear after the first API response in a session. Sign in normally and send a normal Claude Code message; the helper never generates an inference request to obtain quota data. Accounts or windows that do not report these fields remain unavailable.

These are provider quota observations, separate from token totals, billing estimates or a cross-provider usage budget.

## Recorded tokens

Each provider also shows **Mac today**: tokens recorded in its local activity logs for the current Cincinnati calendar day. This covers this Mac's recorded activity, including locally recorded subagents. It does not include another computer, web sessions or deleted/missing local history, and does not estimate the tokens behind an account allowance percentage. These are activity totals, not billable-token equivalents or dollar amounts.

The Mac helper scans recently modified JSONL logs under `~/.codex/sessions` and `~/.claude/projects`, reading only usage-event metadata into the accumulator. Codex uses increments of `total_token_usage.total_tokens` without adding cache/reasoning subcategories a second time; repeated cumulative observations and copied fork events are deduplicated. The first observation or a counter reset uses `last_token_usage` to avoid charging inherited context as new usage. Claude sums input, output, cache-creation input and cache-read input once per API message/request, taking the largest streamed usage observation instead of adding repeated content blocks. Cache reads therefore contribute to the activity count despite their different billing treatment. Claude's status-line context-window totals are not used as lifetime usage; [its documented fields describe the latest context](https://code.claude.com/docs/en/statusline).

The scan streams files and bounds individual records to 1 MiB; oversized or malformed records are skipped. Missing/inaccessible log roots show unavailable or retain the previous timestamped reading. Only the aggregate count, local date and collection timestamp are saved or sent; conversation content, paths and message/request IDs are not retained. Counts use compact K/M/B formatting, are marked saved after 15 minutes, and disappear at the next Cincinnati midnight if no new reading arrives. Existing local history can be incomplete, so the display represents recorded local activity rather than an account audit.

## Mac setup

Requirements: macOS, Python 3.9+, signed-in Codex CLI, Claude Code with its normal sign-in, and an already authorized ADB connection to the TV. From the repository:

```sh
python3 scripts/install-usage-sync.py \
  --codex /absolute/path/to/codex \
  --adb /absolute/path/to/adb \
  --serial YOUR_AUTHORIZED_ADB_TARGET
```

The installer copies the helper into `~/Library/Application Support/VistaUsage`, creates the per-user `com.griffin.vista.usage` LaunchAgent with a five-minute interval, and adds a Claude Code status-line command. Other Claude settings are preserved. An existing different status line is left untouched and causes installation to stop; chain the collector explicitly if you already use one. Its previous value is backed up in the private helper directory. Restart an existing Claude session if it does not reload the setting.

For one manual refresh:

```sh
python3 "$HOME/Library/Application Support/VistaUsage/coding-usage.py" collect \
  --state-dir "$HOME/Library/Application Support/VistaUsage"
```

The Mac must be awake and the TV reachable for updates. The helper does not wake either device, add a cloud service, or solve the bridge's full-reboot startup limitation. ADB access remains the owner's existing privileged development connection.

## Data boundary and rendering

Provider credentials stay in their existing Mac authentication stores. The Claude hook retains no prompts, transcripts, session IDs, project paths or model names. It allowlists percentage, window duration, reset timestamp and observation timestamp; the separate metadata scan adds only daily aggregate token counts. Local helper state is private; do not commit it.

ADB sends a base64-encoded, credential-free snapshot through Android's `content call` command. The import provider requires the platform `DUMP` permission and independently checks Binder's caller UID, allowing only shell UID 2000 or Vista's own UID. Ordinary apps cannot publish observations; queries and all other data operations are unsupported. This avoids Fire OS's shell restrictions on app-specific external storage. There is no TV network listener and Vista requests no new permission.

The provider bounds decoded input to 16 KiB, validates numeric values and timestamps, and reserializes only the known provider/window fields into a private preferences cache on its Binder thread. It then notifies the visible native UI. The screensaver's existing 15-second widget tick updates freshness labels; no additional animation or polling loop is introduced.

An unsuccessful refresh retains the previous source timestamp and marks Codex's fallback as saved. The TV labels readings older than 15 minutes as saved, hides individual reset-passed windows, and hides all numbers after 24 hours without a source observation. Unknown data is shown as Waiting for sync or No limits reported, never as zero. A reported 0% remains valid. There can be up to five minutes of collection latency; freshness labels follow the existing UI refresh cadence.

## Disable

Stop only this helper:

```sh
launchctl bootout "gui/$(id -u)/com.griffin.vista.usage"
rm "$HOME/Library/LaunchAgents/com.griffin.vista.usage.plist"
```

Remove Vista's `statusLine` entry from `~/.claude/settings.json`, or restore the value recorded in `~/Library/Application Support/VistaUsage/original-statusline.json`. Preserve any settings added since installation. The private helper directory can then be removed. The TV's last reading ages out automatically; no provider credentials need removal from the TV because none were sent.
