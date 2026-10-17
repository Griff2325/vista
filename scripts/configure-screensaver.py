#!/usr/bin/env python3
"""Select Vista's system dream and timings, with a device-private rollback snapshot."""
import argparse
import hashlib
import json
import os
import re
import shlex
import time
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
VALUES = {
    "secure": {
        "amazon_ambient_enabled": "0",
        "screensaver_components": "com.griffin.solhome/.SolDreamService",
        "screensaver_default_component": "com.griffin.solhome/.SolDreamService",
        "screensaver_enabled": "1",
        "screensaver_activate_on_sleep": "1",
        "sleep_timeout": "3600000",
    },
    "system": {"screen_off_timeout": "120000"},
}


def configure(adb, serial, restore=False):
    def shell(*args):
        return subprocess.check_output([adb, "-s", serial, "shell", *args], text=True, timeout=25).strip()

    folder = ROOT / ".local" / "screensaver"
    folder.mkdir(parents=True, exist_ok=True, mode=0o700)
    backup = folder / (hashlib.sha256(serial.encode()).hexdigest()[:20] + ".json")
    pidfile = "/data/local/tmp/sol-dream-supervisor.pid"
    pid = shell("cat", pidfile) if "sol-dream-supervisor.pid" in shell("ls", "/data/local/tmp") else ""
    if pid.isdigit():
        try:
            command = shell("cat", "/proc/" + pid + "/cmdline").replace("\x00", " ")
        except subprocess.CalledProcessError:
            command = ""
        if "sol-dream-supervisor.sh" in command:
            shell("kill", pid)
    if restore:
        original = json.loads(backup.read_text())
        # Only the known settings can be restored; backup text is never shell code.
        desired = {ns: {key: original[ns][key] for key in values} for ns, values in VALUES.items()}
    else:
        package = shell("dumpsys", "package", "com.griffin.solhome")
        if "com.griffin.solhome/.SolDreamService" not in package:
            raise RuntimeError("Install Vista 0.5 or later before configuring its screensaver")
        original = json.loads(backup.read_text()) if backup.exists() else {}
        changed = False
        for ns, values in VALUES.items():
            bucket = original.setdefault(ns, {})
            for key in values:
                if key not in bucket:
                    bucket[key] = shell("settings", "get", ns, key)
                    changed = True
        if changed:
            descriptor = os.open(backup, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
            with os.fdopen(descriptor, "w") as output:
                json.dump(original, output, indent=2)
                output.write("\n")
        desired = VALUES
    for namespace, values in desired.items():
        for key, value in values.items():
            if value == "null":
                shell("settings", "delete", namespace, key)
            else:
                shell("settings", "put", namespace, key, str(value))
    for namespace, values in desired.items():
        for key, value in values.items():
            if shell("settings", "get", namespace, key) != str(value):
                raise RuntimeError("Setting readback mismatch: " + key)
    if not restore:
        apk = shell("pm", "path", "com.griffin.solhome").splitlines()[0].replace("package:", "", 1)
        if not re.fullmatch(r"/[A-Za-z0-9_/.~+=-]+\.apk", apk):
            raise RuntimeError("Unexpected installed APK path")
        output = shell("env", "CLASSPATH=" + apk, "app_process", "/system/bin", "com.griffin.solhome.DreamConfig")
        match = re.search(r"SET_DREAM_TRANSACTION=(\d+)", output)
        if not match:
            raise RuntimeError("Could not verify the device's dream-manager transaction")
        remote = "/data/local/tmp/sol-dream-supervisor.sh"
        subprocess.check_call([adb, "-s", serial, "push", str(ROOT / "bridge" / "screensaver-supervisor.sh"), remote], stdout=subprocess.DEVNULL)
        shell("chmod", "700", remote)
        command = "nohup sh " + remote + " " + match[1] + " >/data/local/tmp/sol-dream-supervisor.log 2>&1 </dev/null &"
        shell("sh", "-c", shlex.quote(command))
        time.sleep(.3)
        pid = shell("cat", pidfile)
        if not pid.isdigit() or remote not in shell("cat", "/proc/" + pid + "/cmdline").replace("\x00", " "):
            raise RuntimeError("The local dream supervisor did not start")
    print("Previous screensaver settings restored." if restore else "Vista screensaver selected: 2-minute idle, 60-minute sleep. Settings readback verified.")
    print("Private recovery snapshot: " + str(backup))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()
    configure(args.adb, args.serial, args.restore)
