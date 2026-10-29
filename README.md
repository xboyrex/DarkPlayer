<div align="center">

<img src="docs/assets/logo_circle.png" width="136" height="136" alt="Dark Player Circular Logo" />

# Dark Player

**High-Performance Android Video Player with 1000% Intelligent Dolby DSP, Refractive Liquid Glass UI & NextLib FFmpeg Media3 Engine**

[![Release](https://img.shields.io/github/v/release/xboyrex/DarkPlayer?style=for-the-badge&logo=github&label=LATEST%20RELEASE&color=FACC15&labelColor=090D16)](https://github.com/xboyrex/DarkPlayer/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/xboyrex/DarkPlayer/build.yml?style=for-the-badge&logo=githubactions&label=CI%20BUILD&color=10B981&labelColor=090D16)](https://github.com/xboyrex/DarkPlayer/actions)
[![Platform](https://img.shields.io/badge/ANDROID-7.0%2B%20(API%2024--36)-38BDF8?style=for-the-badge&logo=android&labelColor=090D16)](https://github.com/xboyrex/DarkPlayer/releases/latest)
[![Developer](https://img.shields.io/badge/AUTHOR-MAYANK%20(%40xboyrex)-FACC15?style=for-the-badge&logo=github&labelColor=090D16)](https://github.com/xboyrex)

<br />

<img src="docs/assets/banner.svg" width="100%" alt="Dark Player Hero Banner" />

</div>

<br />

## <img src="docs/assets/ic_download.svg" width="22" height="22" align="absmiddle" /> Direct APK Downloads

Every push to `main` automatically compiles, signs, and publishes multi-ABI release APKs to the [Official GitHub Releases](https://github.com/xboyrex/DarkPlayer/releases/latest) page.

| Package Variant | Target Architecture | Recommended Devices | Direct Download |
| :--- | :--- | :--- | :--- |
| **ARM64 (`arm64-v8a`)** | 64-bit ARMv8-A | All modern Android phones & tablets (Recommended) | [**Download arm64-v8a APK**](https://github.com/xboyrex/DarkPlayer/releases/latest) |
| **ARM32 (`armeabi-v7a`)** | 32-bit ARMv7-A | Older 32-bit Android phones & Android TV boxes | [**Download armeabi-v7a APK**](https://github.com/xboyrex/DarkPlayer/releases/latest) |
| **x86_64 (`x86_64`)** | 64-bit Intel / AMD | Chromebooks, PC Emulators & x86 tablets | [**Download x86_64 APK**](https://github.com/xboyrex/DarkPlayer/releases/latest) |
| **Universal (`universal`)** | All ABIs Combined | Any Android device (`arm64` + `armv7` + `x86` + `x86_64`) | [**Download Universal APK**](https://github.com/xboyrex/DarkPlayer/releases/latest) |

<br />

## <img src="docs/assets/ic_audio.svg" width="22" height="22" align="absmiddle" /> 1000% Intelligent Dolby Audio & Bass Booster Engine

Dark Player features a custom hardware + psychoacoustic multi-stage audio pipeline (`DolbyVolumeBoosterEngine`) engineered for maximum loudness without speaker crackle or digital clipping:

| DSP Stage | Hardware / API Module | Acoustic Function |
| :--- | :--- | :--- |
| **Stage 1: Master Pre-Amp (`0%`–`1000%`)** | `LoudnessEnhancer` + `DynamicsProcessing` | Seamless right-side vertical swipe and slider scaling from `0%`–`100%` system volume up to `1000%` (`+30 dB` / `+3000 mB` hardware target gain). |
| **Stage 2: 5-Band Psychoacoustic EQ** | `DynamicsProcessing.Eq` | Dedicated sub-bass harmonic lift (`90 Hz`–`250 Hz`), dialogue presence isolation (`1.2 kHz`–`4 kHz`), and high-frequency air (`16 kHz`). |
| **Stage 3: Sub-Bass Harmonic Exciter** | `BassBoost` (`0`–`1000` strength) | Deep low-end punch adjustable from `0%` to `100%` inside the player overlay or Hub. |
| **Stage 4: 3D Surround Spatializer** | `Virtualizer` (`850` stage width) | Expands stereo width for an immersive cinema soundstage on both stereo speakers and headphones. |
| **Stage 5: Brickwall Anti-Clip Limiter** | `DynamicsProcessing.Limiter` | `1 ms` fast attack, `45 ms` release, `18:1` ratio ceiling at `-1.2 dBFS` to eliminate harsh distortion at `400%`–`1000%` boost. |

### Built-In Sound Profiles
- **Dolby Cinema**: Balanced 3D surround soundstage, deep sub-bass (`70%`), and vocal clarity enhancement.
- **1000% Extreme**: Maximum `+30 dB` pre-amp amplification locked with brickwall anti-clip limiting.
- **Bass Punch**: Maximum `100%` harmonic sub-bass exciter with warm low-end dynamics.
- **Dialogue Boost**: Midrange vocal isolation (`1 kHz`–`4 kHz`) for quiet movie dialogue and night listening.

<br />

## <img src="docs/assets/ic_glass.svg" width="22" height="22" align="absmiddle" /> Liquid Glass & Solid AMOLED Dual-Mode Interface

- **Real-Time Switchable UI (`Liquid Glass` / `Solid UI`)**: Instant one-tap toggle in the **Hub** header and **Settings -> Appearance** to switch between refractive AGSL **Liquid Glass** (`LiquidBottomTabs` with lens refraction, vibrancy, and damped spring physics) and **Solid Matte AMOLED** surfaces.
- **True Pure AMOLED Black (`#000000`) + Cyber Yellow (`#FACC15`)**: Out-of-the-box pure black background with high-contrast white typography and Cyber Yellow accents on active navigation tabs, floating action buttons, and folder video count badges.
- **Translucent Liquid Glass Player Controls & Overlays**: Semi-transparent dark glass buttons and side/bottom overlay sheets (`Select decoders`, `Select audio track`, `Select subtitle track`, `Chapters`, `Now playing`, `Sleep timer`, and `1000% Dolby Audio Booster`) that let active video playback shine through softly.

<br />

## <img src="docs/assets/ic_video.svg" width="22" height="22" align="absmiddle" /> Playback, Decoders & Smart Tools

- **Triple-Mode Decoder Pipeline (`HW` / `SW+` / `SW`)**: Instant runtime switching and automatic fallback between Hardware MediaCodec (`HW`), Android Software MediaCodec (`SW+`), and bundled **NextLib FFmpeg** (`SW`) for 10-bit HEVC, AV1, VP9, MKV, MP4, AVI, TS, FLV, and Dolby AC-3 / E-AC-3 / DTS / TrueHD audio.
- **Instant First-Frame Keyframe Thumbnails**: Hardware keyframe extraction (`ThumbnailGenerationStrategy.FIRST_FRAME`) with RGB_565 memory optimization and Coil disk/memory caching for zero-lag scrolling.
- **One-Tap Video Frame Screenshot Capture**: Captures the exact current video frame directly to `Pictures/DarkPlayer` and registers it with the Android MediaStore gallery.
- **Built-In Sleep Timer**: Live countdown timer (`10m`, `15m`, `30m`, `45m`, `60m`, `90m`, `120m`) inside the video player that automatically pauses playback when time expires.
- **Fine Playback Speed (`0.25x` – `8.0x`) & Skip Silence**: Precision `0.05x` increment slider, quick preset pills, long-press `2.0x` boost gesture, and automatic silence skipping.

<br />

## <img src="docs/assets/ic_shield.svg" width="22" height="22" align="absmiddle" /> Biometric Private Vault & Network Streaming

- **Biometric & PIN Encrypted Vault**: Hide sensitive videos behind a 4-digit PIN and hardware Fingerprint / Face Unlock biometric authentication, backed by isolated local storage and Recycle Bin (`Trash`) recovery.
- **Multi-Protocol Network Streaming**: Stream directly from **SMB (Samba)**, **FTP**, **SFTP**, **WebDAV**, and direct **HTTP / HTTPS / HLS (`.m3u8`) / DASH (`.mpd`) / RTSP** URLs.
- **In-App GitHub OTA Auto-Updater**: Check for the latest GitHub release, inspect release notes, and download the matching architecture APK directly from the **Hub** tab.

<br />

## <img src="docs/assets/ic_changelog.svg" width="22" height="22" align="absmiddle" /> Release Changelog

### v1.2.0 — Dolby 1000% DSP & Dual-Mode Liquid Glass Release
- **Added**: **1000% Intelligent Dolby Audio & Bass Booster Engine** (`DolbyVolumeBoosterEngine`) with `0%`–`1000%` swipe gesture support, 5-band psychoacoustic EQ, sub-bass harmonic exciter, 3D surround virtualizer, and brickwall anti-clip limiter.
- **Added**: **In-Player & Hub 1000% Dolby Booster Sheet** with instant presets (`100%`, `200%`, `400%`, `600%`, `800%`, `1000% MAX`) and 4 acoustic profiles (`Dolby Cinema`, `1000% Extreme`, `Bass Punch`, `Dialogue Boost`).
- **Added**: **One-Tap Liquid Glass / Solid UI Switcher** in both the **Hub** header and **Settings -> Appearance**.
- **Added**: **In-App GitHub OTA Update Checker** and **Direct Network Stream URL Launcher** in the redesigned compact **Hub** tab.
- **Updated**: Replaced blue navigation/badge highlights with **Cyber Yellow (`#FACC15`)** on Pure AMOLED Black (`#000000`) while keeping player controls and overlays in translucent Liquid Glass with crisp white typography.
- **Automated**: Configured GitHub Actions CI/CD to automatically publish and update the `latest` GitHub Release with multi-ABI APKs and SHA-256 checksums on every push.

<br />

## <img src="docs/assets/ic_dev.svg" width="22" height="22" align="absmiddle" /> Developer & Ecosystem

Crafted and maintained by **ㅤᏞᴀᴢʏㅤᎷꫝʏᴀɴк まやんく (`@xboyrex`)**.

| Platform / Project | Link |
| :--- | :--- |
| **GitHub Profile** | [github.com/xboyrex](https://github.com/xboyrex) |
| **Dark Player Repository** | [github.com/xboyrex/DarkPlayer](https://github.com/xboyrex/DarkPlayer) |
| **Telegram Channel** | [t.me/RoranoaxZORO](https://t.me/RoranoaxZORO) |
| **Mayank Droid** | [mayank-droid.pages.dev](https://mayank-droid.pages.dev) |
| **Onyx Services** | [onyx-services.pages.dev](https://onyx-services.pages.dev) |
| **Otaku Hub Pro** | [otaku-hub-pro.pages.dev](https://otaku-hub-pro.pages.dev) |
| **Portfolio HBO** | [portfolio-hbo.pages.dev](https://portfolio-hbo.pages.dev) |
| **Mi Unlock Portal** | [mi-unlock.pages.dev](https://mi-unlock.pages.dev) |
