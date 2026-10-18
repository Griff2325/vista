#!/usr/bin/env python3
"""Read quota observations locally; send only an allowlisted snapshot to Vista over ADB."""
import argparse
import base64
import json
import math
import os
from pathlib import Path
import selectors
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_STATE = ROOT / '.local' / 'coding-usage'
TV_URI = 'content://com.griffin.solhome.coding-usage'
MAX_BYTES = 16384


def load(path):
    try:
        if path.stat().st_size > MAX_BYTES:
            return {}
        value = json.loads(path.read_text())
        return value if isinstance(value, dict) else {}
    except (OSError, ValueError):
        return {}


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, name = tempfile.mkstemp(prefix='.usage-', dir=str(path.parent))
    try:
        with os.fdopen(fd, 'w') as out:
            json.dump(value, out, separators=(',', ':'), allow_nan=False)
        os.replace(name, path)
    finally:
        if os.path.exists(name):
            os.unlink(name)


def window(used, duration, reset, now):
    if isinstance(used, bool) or isinstance(reset, bool) or isinstance(duration, bool):
        return None
    try:
        used, duration, reset = float(used), int(duration), int(reset)
        if not math.isfinite(used) or not 0 <= used <= 100 or not 1 <= duration <= 527040 or not now < reset * 1000 <= now + 400 * 86400000:
            return None
        return {'usedPercent': used, 'durationMins': duration, 'resetAt': reset * 1000}
    except (TypeError, ValueError, OverflowError):
        return None


def provider(windows, at, status='ok'):
    return {'at': int(at), 'status': status, 'windows': windows[:4]}


def codex_windows(result, now):
    if not isinstance(result, dict):
        raise ValueError('Invalid Codex response')
    buckets = result.get('rateLimitsByLimitId')
    # The main Codex allowance; do not infer which model consumes an extra bucket.
    bucket = buckets.get('codex') if isinstance(buckets, dict) else None
    if not isinstance(bucket, dict):
        bucket = result.get('rateLimits') or {}
    if not isinstance(bucket, dict):
        raise ValueError('Invalid Codex limits')
    windows = []
    for key in ('primary', 'secondary'):
        raw = bucket.get(key)
        if isinstance(raw, dict):
            item = window(raw.get('usedPercent'), raw.get('windowDurationMins'), raw.get('resetsAt'), now)
            if item and item not in windows:
                windows.append(item)
    return provider(sorted(windows, key=lambda w: w['durationMins']), now, 'ok' if windows else 'unavailable')


def claude_windows(payload, now):
    if not isinstance(payload, dict):
        return None
    rate = payload.get('rate_limits')
    if not isinstance(rate, dict):
        return None
    windows = []
    for key, minutes in [('five_hour', 300), ('seven_day', 10080)]:
        raw = rate.get(key)
        if isinstance(raw, dict):
            item = window(raw.get('used_percentage'), minutes, raw.get('resets_at'), now)
            if item:
                windows.append(item)
    return provider(windows, now, 'ok' if windows else 'unavailable')


def read_codex(binary):
    process = subprocess.Popen([binary, 'app-server', '--stdio'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    buffer = b''
    deadline = time.monotonic() + 25

    def send(value):
        process.stdin.write((json.dumps(value) + '\n').encode())
        process.stdin.flush()

    def receive(identifier):
        nonlocal buffer
        while time.monotonic() < deadline:
            while b'\n' in buffer:
                line, buffer = buffer.split(b'\n', 1)
                try:
                    value = json.loads(line)
                except ValueError:
                    continue
                if isinstance(value, dict) and value.get('id') == identifier:
                    if 'error' in value:
                        raise RuntimeError('Codex usage unavailable')
                    return value['result']
            if not selector.select(max(0, deadline - time.monotonic())):
                break
            block = os.read(process.stdout.fileno(), 65536)
            if not block:
                break
            buffer += block
            if len(buffer) > 1048576:
                break
        raise RuntimeError('Codex usage timed out')

    try:
        send({'id': 1, 'method': 'initialize', 'params': {'clientInfo': {'name': 'vista_usage', 'version': '0.8.0'}}})
        receive(1)
        send({'method': 'initialized', 'params': {}})
        send({'id': 2, 'method': 'account/rateLimits/read', 'params': {}})
        return receive(2)
    finally:
        selector.close()
        process.terminate()
        try:
            process.wait(timeout=3)
        except subprocess.TimeoutExpired:
            process.kill(); process.wait()


def clean_saved(raw, now, unavailable=False):
    if not isinstance(raw, dict):
        return provider([], 0, 'waiting')
    windows = []
    at = raw.get('at', 0)
    if not isinstance(at, (int, float)) or isinstance(at, bool) or not 0 < at <= now + 300000:
        return provider([], 0, 'waiting')
    entries = raw.get('windows')
    for entry in (entries if isinstance(entries, list) else [])[:4]:
        if isinstance(entry, dict):
            reset = entry.get('resetAt')
            if not isinstance(reset, (int, float)) or isinstance(reset, bool) or not math.isfinite(reset):
                continue
            value = window(entry.get('usedPercent'), entry.get('durationMins'), reset // 1000, now)
            if value:
                windows.append(value)
    return provider(windows, at, 'saved' if unavailable and windows else 'ok' if windows else 'waiting')


def collect(config, state):
    now = int(time.time() * 1000)
    previous = load(state / 'snapshot.json').get('providers') or {}
    if not isinstance(previous, dict):
        previous = {}
    try:
        codex = codex_windows(read_codex(config['codex']), now)
    except (OSError, RuntimeError, ValueError, KeyError, subprocess.SubprocessError):
        codex = clean_saved(previous.get('codex') or {}, now, True)
    claude = clean_saved(load(state / 'claude.json'), now)
    snapshot = {'schema': 1, 'generatedAt': now, 'providers': {'codex': codex, 'claude': claude}}
    save(state / 'snapshot.json', snapshot)
    adb = config.get('adb')
    serial = config.get('serial')
    if not adb or not serial:
        return snapshot, False
    base = [adb, '-s', serial]
    def run(args, timeout=10):
        return subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout, check=True)
    try:
        try:
            run(base + ['get-state'], 4)
        except (subprocess.SubprocessError, OSError):
            run([adb, 'connect', serial], 5)
        encoded = base64.b64encode(json.dumps(snapshot, separators=(',', ':'), allow_nan=False).encode()).decode('ascii')
        result = run(base + ['shell', 'content', 'call', '--uri', TV_URI, '--method', 'publish', '--arg', encoded])
        if b'accepted=true' not in result.stdout:
            return snapshot, False
        return snapshot, True
    except (subprocess.SubprocessError, OSError):
        return snapshot, False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['collect', 'claude-status', 'configure'])
    parser.add_argument('--state-dir', type=Path, default=DEFAULT_STATE)
    parser.add_argument('--adb')
    parser.add_argument('--serial')
    parser.add_argument('--codex')
    args = parser.parse_args()
    state = args.state_dir.resolve()
    if args.command == 'configure':
        # Config contains local executable paths and the selected device, never provider tokens.
        config = {'codex': args.codex or shutil.which('codex'), 'adb': args.adb or shutil.which('adb'), 'serial': args.serial}
        if not config['codex'] or not config['adb'] or not config['serial']:
            parser.error('Provide Codex, ADB and the already authorized TV serial')
        save(state / 'config.json', config)
        print('Private collector configuration saved.')
    elif args.command == 'claude-status':
        try:
            raw = sys.stdin.buffer.read(1048577)
            if len(raw) > 1048576:
                raise ValueError()
            result = claude_windows(json.loads(raw), int(time.time() * 1000))
            if result is not None:
                save(state / 'claude.json', result)
            # No prompt, transcript, path, account ID, model name or token counts are retained.
            print('Claude Code · Vista usage sync')
        except (ValueError, TypeError, OSError):
            print('Claude Code')
    else:
        snapshot, published = collect(load(state / 'config.json'), state)
        # Keep account values and device addresses out of routine logs.
        print('Usage snapshot ' + ('published to Vista.' if published else 'saved locally; TV unavailable or not configured.'))
        return 0 if published else 1
    return 0

if __name__ == '__main__':
    sys.exit(main())
