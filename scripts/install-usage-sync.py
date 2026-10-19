#!/usr/bin/env python3
"""Install Vista's credential-free TV quota collector as a per-user macOS LaunchAgent."""
import argparse
import json
import os
from pathlib import Path
import plistlib
import shlex
import shutil
import subprocess
import sys
import tempfile

LABEL = 'com.griffin.vista.usage'


def install_claude_plugin(state):
    source = Path(__file__).resolve().parent.parent / 'tools/claude-usage-plugin'
    target = Path.home() / '.claude/skills/vista-usage'
    manifest = target / '.claude-plugin/plugin.json'
    if target.is_symlink() or target.exists() and (not manifest.exists() or json.loads(manifest.read_text()).get('name') != 'vista-usage'):
        raise ValueError('An unrelated ~/.claude/skills/vista-usage directory already exists')
    state.mkdir(parents=True, exist_ok=True);state.chmod(0o700)
    fd, temporary = tempfile.mkstemp(prefix='.collector-', dir=state)
    try:
        with os.fdopen(fd, 'wb') as out:
            out.write(Path(__file__).with_name('coding-usage.py').read_bytes())
        os.chmod(temporary, 0o700)
        os.replace(temporary, state / 'coding-usage.py')
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    shutil.copytree(source, target, dirs_exist_ok=True, ignore=shutil.ignore_patterns('runtime.json', 'tests', '__pycache__'))
    runtime = target / 'runtime.json'
    runtime.write_text(json.dumps({'python': sys.executable, 'collector': str(state / 'coding-usage.py'), 'stateDir': str(state)}))
    runtime.chmod(0o600)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', required=True)
    parser.add_argument('--serial', required=True, help='Already authorized TV ADB target')
    parser.add_argument('--codex', default=shutil.which('codex'))
    parser.add_argument('--state-dir', type=Path, default=Path.home() / 'Library/Application Support/VistaUsage')
    parser.add_argument('--claude-desktop', action='store_true', help='Also install the quota-only Claude Code mod for new Desktop Code sessions (Claude Code 2.1.302+)')
    args = parser.parse_args()
    if not args.codex:
        parser.error('Codex CLI is required')
    state = args.state_dir.expanduser().resolve();state.mkdir(parents=True, exist_ok=True);state.chmod(0o700)
    source = Path(__file__).resolve().with_name('coding-usage.py')
    installed = state / 'coding-usage.py'
    settings_path = Path.home() / '.claude/settings.json'
    settings = json.loads(settings_path.read_text()) if settings_path.exists() else {}
    command = shlex.join([sys.executable, str(installed), 'claude-status', '--state-dir', str(state)])
    expected = {'type': 'command', 'command': command}
    original = settings.get('statusLine')
    if original and original != expected:
        parser.error('A different Claude status line exists. Preserve it and chain the collector explicitly instead of replacing it.')
    shutil.copyfile(source, installed);installed.chmod(0o700)
    if args.claude_desktop:
        install_claude_plugin(state)
    config = {'codex': str(Path(args.codex).resolve()), 'adb': str(Path(args.adb).resolve()), 'serial': args.serial}
    (state / 'config.json').write_text(json.dumps(config));(state / 'config.json').chmod(0o600)
    backup = state / 'original-statusline.json'
    if not backup.exists():
        backup.write_text(json.dumps({'present': 'statusLine' in settings, 'value': original}));backup.chmod(0o600)
    settings['statusLine'] = expected
    settings_path.parent.mkdir(parents=True, exist_ok=True)
    temp = settings_path.with_name('.vista-settings.tmp');temp.write_text(json.dumps(settings, indent=2)+'\n');temp.chmod(0o600);temp.replace(settings_path)
    agent_path = Path.home() / 'Library/LaunchAgents' / (LABEL + '.plist');agent_path.parent.mkdir(parents=True, exist_ok=True)
    agent = {'Label': LABEL, 'ProgramArguments': [sys.executable, str(installed), 'collect', '--state-dir', str(state)], 'WorkingDirectory': str(state), 'RunAtLoad': True, 'StartInterval': 300, 'ProcessType': 'Background', 'LowPriorityIO': True, 'StandardOutPath': '/dev/null', 'StandardErrorPath': '/dev/null', 'EnvironmentVariables': {'PATH': '/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin'}}
    agent_path.write_bytes(plistlib.dumps(agent));agent_path.chmod(0o600)
    domain = 'gui/' + str(os.getuid())
    subprocess.run(['launchctl', 'bootout', domain + '/' + LABEL], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run(['launchctl', 'bootstrap', domain, str(agent_path)], check=True)
    print('Vista usage sync installed: five-minute Mac refresh and Claude status-line capture. Provider credentials remain in their existing apps.')
    if args.claude_desktop:
        print('Claude Desktop quota mod installed; it observes API responses in new Code sessions only.')

if __name__ == '__main__':
    main()
