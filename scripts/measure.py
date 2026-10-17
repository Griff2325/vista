#!/usr/bin/env python3
"""Short on-device gfxinfo/meminfo sample; requires an already authorized ADB target."""
import argparse
import json
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True, help='ADB serial/address; never stored in output')
parser.add_argument('--out', type=Path, default=Path('build/measurements'))
args = parser.parse_args()
args.out.mkdir(parents=True, exist_ok=True)
base = ['adb', '-s', args.serial, 'shell']
def run(*command):
    return subprocess.run(base + list(command), check=True, capture_output=True,
                          text=True, timeout=20).stdout
run('dumpsys', 'gfxinfo', 'com.griffin.solhome', 'reset')
for index in range(24):
    run('input', 'keyevent', '22' if index % 2 == 0 else '21')
    time.sleep(.12)
(args.out / 'navigation.txt').write_text(run('dumpsys', 'gfxinfo', 'com.griffin.solhome'))
run('dumpsys', 'gfxinfo', 'com.griffin.solhome', 'reset')
time.sleep(3)
(args.out / 'idle.txt').write_text(run('dumpsys', 'gfxinfo', 'com.griffin.solhome'))
(args.out / 'memory.txt').write_text(run('dumpsys', 'meminfo', 'com.griffin.solhome'))
print(f'Saved captures to {args.out}; inspect before publishing device dumps.')
