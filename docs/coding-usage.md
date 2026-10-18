# Coding usage

Home's bottom console shows a compact allowance meter for Codex and Claude Code. Move right from Watching to select it, or open Settings → Coding usage for all available windows, reset times and the age of each reading. The screensaver shows up to two windows per provider beneath the clock. Percentages mean **used**, not remaining. All reset times use America/New_York, the same Cincinnati timezone as the scene.

## Sources

The optional Mac helper uses two documented local interfaces:

- Codex [`account/rateLimits/read`](https://developers.openai.com/codex/app-server), through a short-lived `codex app-server --stdio` process using the existing CLI sign-in. It prefers the main `rateLimitsByLimitId.codex` allowance and falls back to `rateLimits`. Window duration determines whether a reading is labeled 5h, Weekly or another duration; primary/secondary ordering is not treated as a duration guarantee. Additional model-specific buckets are not combined.
- Claude Code's official [`statusLine` input](https://code.claude.com/docs/en/statusline), using only `rate_limits.five_hour` and `rate_limits.seven_day`. These fields depend on account eligibility and appear after the first API response in a session. Sign in normally and send a normal Claude Code message; the helper never generates an inference request to obtain quota data. Accounts or windows that do not report these fields remain unavailable.

These are provider quota observations, not token totals, billing estimates or a cross-provider usage budget.

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

Provider credentials stay in their existing Mac authentication stores. The Claude hook retains no prompts, transcripts, session IDs, project paths, model names or token counts. It allowlists percentage, window duration, reset timestamp and observation timestamp into a small local JSON snapshot. Local helper state is private; do not commit it.

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
