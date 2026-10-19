#!/usr/bin/env python3
"""Render four original, quiet navigation ticks and short scrolling auditions (stdlib only)."""
import math
from pathlib import Path
import random
import struct
import wave

RATE = 44100
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/design-concepts/menu-sounds'

def render(kind):
    duration = {'felt-tap': .085, 'pebble': .105, 'dew': .135, 'pixel-blip': .095}[kind]
    count = round(RATE * duration)
    rng = random.Random(7)
    samples = []
    for i in range(count):
        t = i / RATE
        if kind == 'felt-tap':
            value = (.72 * math.sin(math.tau * 210 * t) * math.exp(-t * 82)
                     + .18 * math.sin(math.tau * 1250 * t) * math.exp(-t * 260)
                     + .09 * rng.uniform(-1, 1) * math.exp(-t * 580))
        elif kind == 'pebble':
            value = sum(gain * math.sin(math.tau * hz * t) * math.exp(-t * decay)
                        for gain, hz, decay in [(.65, 640, 65), (.24, 1090, 110), (.1, 1870, 170)])
        elif kind == 'dew':
            phase = math.tau * (670 * t + 290 * .012 * (1 - math.exp(-t / .012)))
            value = (.72 * math.sin(phase) * math.exp(-t * 48)
                     + .12 * math.sin(phase * 1.97) * math.exp(-t * 115))
        else:
            phase = math.tau * (520 * t + 110 * .006 * (1 - math.exp(-t / .006)))
            value = (math.sin(phase) + .11 * math.sin(3 * phase) + .025 * math.sin(5 * phase)) * math.exp(-t * 76)
        # Short, smooth edges prevent speaker clicks unrelated to the designed sound.
        fade = min(1, t / .0015) * min(1, (count - 1 - i) / (RATE * .012))
        samples.append(value * max(0, fade))
    rms = math.sqrt(sum(x*x for x in samples) / count)
    gain = min(.035 / rms, .20 / max(abs(x) for x in samples))
    pcm = [round(x * gain * 32767) for x in samples]
    assert pcm[0] == pcm[-1] == 0
    assert 0 < max(abs(x) for x in pcm) <= 6554
    assert abs(sum(pcm) / count / 32767) < .005
    return pcm

def double_level(pcm):
    samples = [value / 32767 for value in pcm]
    rms = lambda values: math.sqrt(sum(value * value for value in values) / len(values))
    target = 2 * rms(samples)
    low, high = 0., 32.
    limited = lambda drive: [.98 * math.tanh(drive * value / .98) for value in samples]
    assert rms(limited(high)) > target
    # Match twice the previous RMS while soft-limiting peaks below full scale.
    for _ in range(44):
        drive = (low + high) / 2
        if rms(limited(drive)) < target:
            low = drive
        else:
            high = drive
    result = [round(value * 32767) for value in limited(high)]
    assert abs(rms(result) / rms(pcm) - 2) < .001
    assert result[0] == result[-1] == 0 and max(abs(value) for value in result) < 32767
    return result


def write(path, pcm):
    with wave.open(str(path), 'wb') as audio:
        audio.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        audio.writeframes(struct.pack('<' + 'h' * len(pcm), *pcm))
    with wave.open(str(path), 'rb') as audio:
        assert audio.getnchannels() == 1 and audio.getsampwidth() == 2
        assert audio.getnframes() == len(pcm)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for kind in ('felt-tap', 'pebble', 'dew', 'pixel-blip'):
        pcm = render(kind)
        write(OUT / (kind + '.wav'), pcm)
        if kind == 'felt-tap':
            deployed = double_level([round(value * 5) for value in pcm])
            assert max(abs(value) for value in deployed) < 32767
            asset = ROOT / 'app/assets/audio/focus.wav'
            asset.parent.mkdir(parents=True, exist_ok=True)
            write(asset, deployed)
        preview = [0] * round(RATE * 2.6)
        for seconds in (.18, .65, 1.12, 1.45, 1.65, 1.85, 2.05):
            offset = round(seconds * RATE)
            for i, value in enumerate(pcm):
                preview[offset + i] += value
        assert max(abs(x) for x in preview) <= 6554
        write(OUT / (kind + '-scroll.wav'), preview)
        print(f'{kind}: {len(pcm)/RATE*1000:.0f} ms, peak {max(abs(x) for x in pcm)/32767:.3f}, {len(pcm)*2} PCM bytes')

if __name__ == '__main__':
    main()
