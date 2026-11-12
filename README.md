<div align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" height="128" alt="Nova Player Logo" />
  <h1>Nova Player</h1>
  <p><b>A native Android video player built in Kotlin & Jetpack Compose with Liquid Glass UI, Pure AMOLED Black / Pure White themes, and an integrated Dolby Atmos & 20-Band Graphic Equalizer Studio.</b></p>
</div>

## Overview

**Nova Player** is a fast, ad-free, open-source video player for Android phones, tablets, and Android TV. Built with Kotlin, Jetpack Compose, and Media3 ExoPlayer + FFmpeg decoders, it combines local and network media management with a refractive **Liquid Glass UI** and a built-in **Dolby Atmos & 20-Band Graphic Equalizer Studio**.

## Supported Formats

* **Video**: H.263, H.264 AVC, H.265 HEVC, MPEG-4 SP, VP8, VP9, AV1, HDR10, HDR10+, Dolby Vision
* **Audio**: Vorbis, Opus, FLAC, ALAC, PCM/WAVE (μ-law, A-law), MP1, MP2, MP3, AMR (NB, WB), AAC (LC, ELD, HE, xHE), AC-3, E-AC-3, E-AC-3 JOC (Dolby Atmos), AC-4, DTS, DTS-HD, TrueHD (via bundled FFmpeg extension)
* **Subtitles**: SRT, SSA, ASS (with embedded font & style rendering), TTML, VTT, DVB, VobSub, PGS

## Key Features

* **Liquid Glass UI & Motion**: Frosted refractive glass navigation bar with damped drag-to-switch pill, interactive press shaders, spring-physics animations, and tactile haptic feedback across controls.
* **Strictly Dual High-Contrast Color Modes**: Instant switching between **Pure AMOLED Black (`#000000`)** and **Pure White (`#FFFFFF`)** modes, plus optional Dynamic Wallpaper accents and custom typography.
* **Nova Dolby Atmos & Equalizer Studio**:
  * **Hardware & Software DAP**: Supports vendor Dolby Audio Processing (`libswdap.so` / `libhwdap.so`) alongside universal Android `DynamicsProcessing` multi-band DSP.
  * **7 Acoustic Profiles**: Dynamic, Movie/Video, Music, Game, Work, Casual, and Mood.
  * **Intelligent Equalizer (IEQ)**: Off, Detailed, Balanced, and Warm real-time tonal targets.
  * **10 / 15 / 20-Band Graphic Equalizer**: Interactive draggable cubic Bézier frequency response curve, vertical band sliders, and a `0.1 dB` precision band tuner (`±15.0 dB`).
  * **19 Built-in Presets & Custom Presets**: Flat, Rock, Jazz, Pop, Classical, Hip Hop, Blues, Electronic, Metal, Acoustic, Bass Boost, Beats, Crystal Clear, Deep, Dubstep, Hardstyle, Movie, R&B, Vocal Booster, plus JSON preset import/export.
  * **Advanced Audio Enhancers**: Bass Enhancer (with Balanced, Sub Boost, and Punch curves), Mid Enhancer, Treble Enhancer, Headphone & Speaker Surround Virtualizer (`4–64` stereo widening), Dialogue Enhancer (`1–12` clarity), Volume Leveler, and Per-Device Audio Memory.
* **Advanced Playback & Decoder Switching**: Runtime switching between Hardware (`MediaCodec`), System Software, and Bundled FFmpeg decoders, plus 4K HDR window output and high-refresh-rate display mode locking.
* **Media Picker & Private Vault**: Browse media in Folder, Tree, or Video views (List or Grid layouts), or lock sensitive videos inside a PIN & Biometric-protected private Vault.
* **Network Streaming & Playlists**: Stream directly from SMB, FTP, SFTP (with SSH key verification), WebDAV, HTTP/HTTPS URLs, and local or remote M3U playlists.
* **Touch Gestures & PiP**: Vertical swipe for brightness (left) and volume (right), horizontal swipe to seek, pinch-to-zoom & pan, double-tap seek/play/pause, long-press `2.0x` speed boost, Picture-in-Picture (PiP), background audio playback, and Android TV D-pad navigation.
* **In-App OTA Updater & Storage Cleaner**: Built-in GitHub Releases update checker/installer and real-time storage analyzer with one-tap cache cleaning.

## Player Intent API

Nova Player supports standard Android `ACTION_VIEW` intents with MX Player-compatible extras for external integrations, position resume, custom headers, subtitle tracks, and background playback. See [PLAYER_API.md](PLAYER_API.md) for full integration details.

## Build Instructions

Clone the repository and assemble the debug or release APKs using the Gradle wrapper:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

The generated universal and per-ABI APKs are written to `app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`.

## Developer & Community

Developed and maintained by **Mayank** (`ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく` · [`@xboyrex`](https://github.com/xboyrex)):

* **GitHub**: [github.com/xboyrex](https://github.com/xboyrex)
* **Telegram Channel**: [t.me/MayankDroid7](https://t.me/MayankDroid7)
* **Onyx Hub Community**: [t.me/OnyxHub7](https://t.me/OnyxHub7)
* **YouTube (Mayank Droid)**: [youtube.com/@mayankdroid](https://youtube.com/@mayankdroid?si=slX8DDU0dpLSIYyY)
* **YouTube (Onyx Droid)**: [youtube.com/@onyxdroid89](https://youtube.com/@onyxdroid89?si=9gxpZfa3bpI9PKku)
* **Instagram**: [instagram.com/mayank.zx7](https://www.instagram.com/mayank.zx7)
* **Email**: [xboyrex496@gmail.com](mailto:xboyrex496@gmail.com)

## License

Nova Player is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for full details.
