"""Patch one verified public player version; never distribute its generated output."""
import hashlib
from pathlib import Path
import sys

EXPECTED_SHA256 = 'a5f28f8679dca082475d1500953e2169226107576114f6ec1b0cef38f0a35d88'
ENTRY = '.prototype.onEnterBreak=function(e){var t;return null==e&&(e={}),'
VOD_SKIP = 'this.getStateMachine().isLinearTime()||(e.shouldSkipPod=!0,window.__vistaHuluLabSkipped&&window.__vistaHuluLabSkipped()),'
BADGE = '''\n;(function(){var count=0;function show(){var badge=document.getElementById("vista-hulu-lab");if(!badge){badge=document.createElement("div");badge.id="vista-hulu-lab";badge.style.cssText="position:fixed;right:24px;top:18px;z-index:2147483647;background:#123b2c;color:#fff;padding:8px 12px;font:18px sans-serif;pointer-events:none";document.body.appendChild(badge)}badge.textContent="HULU LAB / PLAYER PATCH / SKIPS "+count}window.__vistaHuluLabSkipped=function(){count++;show()};window.setTimeout(show,0)})();\n'''


def patch(data):
    if hashlib.sha256(data).hexdigest() != EXPECTED_SHA256:
        raise ValueError('Player version mismatch; refusing to patch')
    text = data.decode('utf-8')
    if text.count(ENTRY) != 1:
        raise ValueError('Expected exactly one ad-entry method')
    return (text.replace(ENTRY, ENTRY + VOD_SKIP) + BADGE).encode('utf-8')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit('Usage: patch_player.py ORIGINAL.js PRIVATE_OUTPUT.js')
    source, target = map(Path, sys.argv[1:])
    if source.resolve() == target.resolve():
        raise SystemExit('Output must differ from original')
    result = patch(source.read_bytes())
    target.write_bytes(result)
    print('Version-pinned on-demand ad-entry patch generated; live/recorded paths retained.')
