# Menu navigation sound auditions

Historical sound auditions. As of 0.11.5 (October 27, 2026), Vista Home navigation is silent at the owner’s request. These previews remain as design references; no sample is bundled or played by the app.

| Option | Character | Single tick |
| --- | --- | --- |
| [Felt tap](felt-tap-scroll.mp3) | Soft, low tactile click | 85 ms |
| [Pebble](pebble-scroll.mp3) | Hollow, wooden resonance | 105 ms |
| [Dew](dew-scroll.mp3) | Smooth, rounded water drop | 135 ms |
| [Pixel blip](pixel-blip-scroll.mp3) | Short, playful retro pulse | 95 ms |

Regenerate these reference WAV auditions with `python3 tools/generate-menu-sounds.py` (Python standard library only). It no longer writes into app assets. MP3 previews were encoded from the `*-scroll.wav` auditions with FFmpeg/libmp3lame at 128 kbps. The renderer checks PCM bounds, silent endpoints, DC offset and output format.
