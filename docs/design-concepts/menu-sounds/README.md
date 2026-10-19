# Menu navigation sound auditions

Four original synthesized focus ticks for Vista. Each MP3 audition plays the same seven-step sequence, starting slowly and ending with faster scrolling; the individual mono 44.1 kHz/16-bit WAV files are the lightweight app assets. The owner selected Felt tap; its WAV is copied to `app/assets/audio/focus.wav` for Home focus changes. The other options remain auditions.

| Option | Character | Single tick |
| --- | --- | --- |
| [Felt tap](felt-tap-scroll.mp3) | Soft, low tactile click | 85 ms |
| [Pebble](pebble-scroll.mp3) | Hollow, wooden resonance | 105 ms |
| [Dew](dew-scroll.mp3) | Smooth, rounded water drop | 135 ms |
| [Pixel blip](pixel-blip-scroll.mp3) | Short, playful retro pulse | 95 ms |

Regenerate the WAV assets and auditions with `python3 tools/generate-menu-sounds.py` (Python standard library only). MP3 previews were encoded from the `*-scroll.wav` auditions with FFmpeg/libmp3lame at 128 kbps. The renderer checks PCM bounds, silent endpoints, DC offset and output format. Preview peak amplitude stays below 0.20 full scale; similar RMS levels support comparison. The deployed tap uses the media output with sonification content, preserving TV/soundbar volume. The native SoundPool loads the sample when Home becomes active, limits feedback to actual focus changes at most once per 100 ms and releases its pool when Home pauses or is destroyed. After the owner requested several level increases, 0.10.5 doubles the measured RMS level (+6 dB) relative to 0.10.4, with offline soft limiting below full scale. This makes the tap denser without adding runtime audio processing or changing its duration. The renderer checks the 2× RMS ratio, silent endpoints and unclipped PCM. The final soundbar level and character still need listening confirmation.
