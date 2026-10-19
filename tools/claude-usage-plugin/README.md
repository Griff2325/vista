# Vista Claude usage mod

Tested with Claude Code 2.1.302. The mod observes Claude Desktop's **Code** sessions and terminal Claude Code sessions that load user plugins. It needs no model call, credential, OAuth access, transcript, prompt, session ID, or network request of its own.

After each main-session `turn.step` finishes streaming with response `usage` (evidence of an API response), it calls `$.session.usage()` without a breakdown. It forwards only `five_hour` and `seven_day` quota percentages and reset times to Vista's `claude-mod` helper command over process stdin. The Python helper validates again and atomically saves the private `claude.json` with mode 0600; its exit status reports whether saving succeeded. A missing quota, expired reset, failed helper, response without usage, or subagent response leaves the saved observation alone. All stream chunks and the response result pass through unchanged. Collector failures do not alter the response; the helper has a two-second timeout. A multi-tool turn can therefore update quotas after each API response while the overall task is still working.

There is no startup quota capture or timer. `$.session.usage()` exposes the limits reported by the last API response; it does not fetch a live account-wide quota. Readings stay old until a normal API response is observed in a session that loaded this mod. Daily token totals still use the separate local JSONL scan. Other Claude Desktop tabs and remote/container Code sessions that lack this Mac's helper are outside this mod's coverage.

## Install and reload

Run the existing `scripts/install-usage-sync.py` command with its normal ADB/Codex arguments plus `--claude-desktop`. The option copies this plugin into `~/.claude/skills/vista-usage` and writes a private `runtime.json` pointing to the installed Python helper. An existing unrelated directory at that path causes installation to fail. The normal terminal status-line capture and five-minute publisher remain installed.

For an existing collector installation, the plugin-only operation is `install_claude_plugin(state)` in that installer; `state` must be the resolved private collector directory. It also atomically updates `coding-usage.py` so the mod and helper use the same command interface. It does not change the LaunchAgent or existing Claude settings.

Claude Code's `claude plugin init --help` documents `~/.claude/skills/<name>` as auto-loading in the next session under `<name>@skills-dir`. Start a **new** Claude Desktop Code session after installation and confirm `vista-usage@skills-dir` is present in `/plugin`. If the already-running Desktop app keeps the old plugin inventory, quit and reopen it once current work is saved, then create a new Code session. Do not interrupt or replace a running conversation merely to load the collector. An ordinary subsequent user turn supplies the first observation; installation itself must not update quota timestamps. The existing five-minute publisher sends that observation to Vista.

## Diagnose a Desktop session

Desktop includes its own Claude Code engine. Check `/status` **inside Desktop**, not just `claude --version` in a terminal; the official docs require Desktop's engine to be 2.1.286 or later for mods. An enabled plugin inventory does not prove that its hooks module loaded. Official docs describe marketplace installation, while the installed CLI also documents skills-dir auto-loading; actual loading in a Desktop session must be verified.

Run `/vista-usage-check` in the target Desktop Code session. It is a native command with no inference: it reports that the mod loaded, tests the configured Python helper with `--help`, and shows the latest sanitized stage. It never changes quota data or its timestamp. An unavailable command means the module did not register it; check plugin loading, Desktop's engine version, policy/rollout restrictions, and use `/reload-plugins` where supported. A terminal's `/plugin` view has a separate **mods active** line; an enabled entry alone is insufficient.

The private collector directory's `claude-plugin-health.json` holds one latest diagnostic snapshot, written on module startup and after each main API response. Its only values are a fixed stage, local diagnostic time, `hasUsage`, bounded window/error counters, and an allowlisted error class. No IDs, prompt text, transcript, tokens, provider credentials, or raw error messages are retained. The parent directory is mode 0700. This diagnostic write is best effort and can be partial; it does not share the atomic quota file.

Stages distinguish `loaded`, `no-response-usage`, `usage-read` with an error class, `limits-unavailable`, `no-valid-limits` with invalid percent/reset counters, `helper-run` with an error class, `helper-exit-error`, and `saved`. `runtime-error` appears in the diagnostic command when the local helper configuration cannot be read. Health time proves only that a diagnostic ran; the TV continues to use the quota observation's separate timestamp.

Remove `~/.claude/skills/vista-usage` to uninstall the mod, then use a new session. The collector and terminal hook are separate and can remain.

## Local verification

```sh
claude plugin validate tools/claude-usage-plugin
claude plugin test tools/claude-usage-plugin
python3 -m unittest discover -s tests -p test_coding_usage.py
```

The plugin test runner uses stubs without sign-in, network, or inference. A passing test does not prove that an already-running Desktop session loaded the mod or has received fresh headers.

API references: [mods reference](https://code.claude.com/docs/en/plugins/mods/reference), [mods API](https://code.claude.com/docs/en/plugins/mods/api), [Desktop versions and mod loading](https://code.claude.com/docs/en/plugins/mods/overview), [loading failures](https://code.claude.com/docs/en/plugins/mods/troubleshoot), and [official declarations](https://github.com/anthropics/claude-code/blob/main/mods/types/claude-code.d.ts). In the declarations, `SessionRateLimit.resetsAt` is ISO 8601; the collector's `resets_at` input is Unix seconds. The mod converts once at the boundary.
