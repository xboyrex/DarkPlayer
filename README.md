<p align="center">
  <img src="app/src/main/res/drawable/ic_darkplayer_logo.png" alt="Dark Player Logo" width="140" />
</p>

<h1 align="center">Dark Player</h1>

<p align="center">
  <strong>A Navy Dark &amp; Liquid Glass Android video player powered by Kotlin, Jetpack Compose, and Media3.</strong>
</p>

## Highlights

- **Navy Dark Graphite UI:** Engineered around a 7-shade high-contrast palette (`#0E0E10` background, `#232327` surfaces, `#3E3E45` secondary cards, `#63636E` hairline borders, `#F2F2F5` primary text/icons).
- **Liquid Glass Navigation Bar:** Floating frosted-glass bottom capsule using Android 12+ `RenderEffect` blur, translucent rim lighting, spring-animated active pill, and tactile haptics.
- **Redesigned Cinema Player Controls:** Instant access to HW/SW Decoder switching, HDR/SDR toggle, Subtitles, Audio Tracks, Playlist, PIP, Pitch-Locked 2x Speed, Lock, Rotate, Resize (Crop/Fit/Stretch/100%), and Frame Screenshot.
- **Zero-Glitch Aspect Ratio Engine:** Fixes double-density scaling bugs and clips zoomed/cropped frames cleanly with a non-overlapping top-center status pill.
- **Hardware Frame Screenshot:** Captures hardware-accelerated `SurfaceView` frames via `PixelCopy` and saves lossless PNGs to `Pictures/DarkPlayer/`.
- **HDR / SDR Auto-Detection & Toggle:** Probes hardware HEVC/VP9/AV1 10-bit decoders and switches `Window.colorMode` live.
- **Pitch-Locked 2x Speed:** Locks audio pitch at `1.0f` across all playback speeds (long-press the speed pill for instant 2.0x).
- **Built-in GitHub Releases Auto-Updater:** Checks for new releases, displays release notes, downloads the APK with live progress, and launches the system installer.

## Installation

### Option 1: Install Pre-Built APK
1. Download the latest `DarkPlayer.apk` from [GitHub Releases](https://github.com/xboyrex/DarkPlayer/releases/latest).
2. Open the APK on your Android device (Android 7.0+ / API 24+; Liquid Glass blur requires Android 12+ / API 31+).
3. Allow installation from unknown sources if prompted, then tap **Install**.

### Option 2: Automated Build via GitHub Actions
1. Push this repository to GitHub (`main` branch).
2. *(Optional)* Add signing secrets under **Settings → Secrets and variables → Actions**:
   - `KEYSTORE_BASE64` — Base64-encoded release `.jks` / `.keystore` file (`base64 -w0 release.keystore`)
   - `KEYSTORE_PASSWORD` — Keystore password
   - `KEY_ALIAS` — Key alias
   - `KEY_PASSWORD` — Key password
   *(If omitted, CI safely signs with the included debug keystore so builds never fail.)*
3. Open the **Actions** tab → **Dark Player CI / Release Build** and download `DarkPlayer-Release-APKs`.

### Option 3: Build Locally from Source
```bash
git clone https://github.com/xboyrex/DarkPlayer.git
cd DarkPlayer
./gradlew :app:assembleRelease
```
Release APKs are output to `app/build/outputs/apk/release/`.

## License

Released under the GNU General Public License v3.0.
