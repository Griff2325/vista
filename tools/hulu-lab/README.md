# Hulu Lab experiment

Private experimental Hulu testing uses a separate application ID. The original installed app and its data are retained. This directory contains original patch-generation, routing, diagnostics and tests. It contains no Hulu APK, decompiled source, player bundle, native library, account data or signing material. Generated player output must remain private.

## Player patch

`patch_player.py` accepts exactly one public player bundle SHA-256. It inserts an on-demand-only condition into the ad-entry method, selecting the player's existing skip-pod transition. The original transition preserves the return segment/position. Live TV and recordings retain their original path. A small Lab badge counts entries into this experimental skip branch; the count does not prove successful playback. Authentication, subscription entitlements and video-license handling are unchanged.

`player-route.js`, when bundled privately with `observe.js`, starts a loopback-only asset server inside the Lab process. It routes exactly the pinned public JavaScript URL to the APK's private patched copy. It serves only `/player.js`, accepts at most two concurrent connections, bounds request headers and times out idle transfers. No Mac server, proxy, system CA, TLS-verification bypass or root access is required. Other URLs retain their original destination and trust behavior. A new upstream bundle URL falls through to the original player. Unknown input hashes are rejected by the patch generator.

This is a playback experiment, not verified ad removal. Actual protected playback requires official Hulu activation of the separate copy. Native hooks, server enforcement or client updates may prevent the skip from working. The original app is the fallback.

## Diagnostic

`observe.js` alone opens no server and changes no requests. It logs HLS/DASH/other plus allowlisted public Hulu hosts. It additionally logs tightly scoped public app-code paths under Hulu's `semver`/`build_files` trees, with query strings removed. Account/content paths, headers, payloads and unknown host names are omitted. Distinct output is bounded. The JavaScriptCore observer reports only whether a large script passes through its exported evaluation API, without reading script text.

## Checks

```sh
node tools/hulu-lab/observe.test.js
node tools/hulu-lab/player-route.test.js
python3 tools/hulu-lab/patch_player_test.py
python3 tools/hulu-lab/patch_player.py PRIVATE_ORIGINAL.js PRIVATE_PATCHED.js
node --check PRIVATE_PATCHED.js
node tools/hulu-lab/check_player.js PRIVATE_PATCHED.js
```

The last check evaluates only the extracted ad-entry function with stubbed state/transition objects. It checks on-demand skips, preserved return position, null options and unchanged live/recorded paths. It makes no Hulu or playback request. These tools are not part of Vista's Android build.

References: [Frida Gadget autonomous scripts](https://frida.re/docs/gadget/#script), [Frida socket and file APIs](https://frida.re/docs/javascript-api/#socket).
