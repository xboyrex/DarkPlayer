<div align="center">
  <img src="docs/assets/dark-player/dark-player-mark.svg" width="112" height="112" alt="Dark Player logo" />
  <h1>Dark Player</h1>
  <p><strong>A high-control Android video player for local libraries, network media, and deeply configurable playback.</strong></p>
  <p>NextLib + Media3 / FFmpeg engine · Dark-first interface · Liquid Glass-inspired surfaces</p>
  <p>
    <a href="https://github.com/xboyrex/DarkPlayer/releases"><img alt="GitHub releases" src="https://img.shields.io/github/v/release/xboyrex/DarkPlayer?style=for-the-badge&logo=github&label=release" /></a>
    <a href="https://github.com/xboyrex/DarkPlayer/releases"><img alt="GitHub downloads" src="https://img.shields.io/github/downloads/xboyrex/DarkPlayer/total?style=for-the-badge&logo=android&label=downloads" /></a>
    <a href="https://github.com/xboyrex/DarkPlayer/commits"><img alt="Last commit" src="https://img.shields.io/github/last-commit/xboyrex/DarkPlayer?style=for-the-badge&logo=git" /></a>
    <img alt="Platform Android" src="https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  </p>
  <p>
    <a href="#features">Features</a> &nbsp;·&nbsp;
    <a href="#interface-preview">Interface preview</a> &nbsp;·&nbsp;
    <a href="#screenshots">Screenshots</a> &nbsp;·&nbsp;
    <a href="#installation">Installation</a> &nbsp;·&nbsp;
    <a href="#project-links">Project links</a>
  </p>
</div>

---

## Interface preview

<div align="center">
  <img src="docs/assets/dark-player/dark-player-showcase.gif" alt="Animated preview made from Dark Player interface screenshots" width="960" />
  <p><sub>Animated overview assembled from the supplied app screenshots. It demonstrates captured screens, not a live recording of interactions.</sub></p>
</div>

## Overview

**Dark Player** is an Android video-player project focused on direct access to media, configurable playback, and a high-contrast dark interface. The supplied UI captures show a folder-oriented media library, a dedicated Hub, full-screen playback controls, subtitle and decoder selectors, network-stream setup, playlists, and granular settings.

The visual system uses black surfaces, restrained yellow accents, clear iconography, and compact navigation. The app-icon assets supplied with this repository package are preserved in their original Android, iOS, and web variants; an editable SVG mark is included for README and web use.

## Features

<table>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/library.svg" width="32" height="32" alt="Library icon" />
      <h3>Local media library</h3>
      Folder-first browsing, media counts, storage context, and a dedicated view for files within a folder.
    </td>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/video.svg" width="32" height="32" alt="Video icon" />
      <h3>Playback controls</h3>
      Full-screen landscape playback plus a decoder-selection interface captured in the supplied screenshots.
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/network.svg" width="32" height="32" alt="Network icon" />
      <h3>Network media</h3>
      The connection screen lists SMB, FTP, SFTP, and WebDAV for browsing and playing media over a network.
    </td>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/subtitles.svg" width="32" height="32" alt="Subtitle icon" />
      <h3>Audio and subtitles</h3>
      Dedicated subtitle-track selection and configurable audio/subtitle sections in Settings.
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/playlist.svg" width="32" height="32" alt="Playlist icon" />
      <h3>Playlists</h3>
      A separate Playlists destination; the empty-state copy references local playlists and M3U sources.
    </td>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/audio.svg" width="32" height="32" alt="Audio icon" />
      <h3>Audio hub</h3>
      A dedicated audio-booster panel is visible in the screenshots. Extreme gain may introduce clipping and unsafe listening levels.
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/settings.svg" width="32" height="32" alt="Settings icon" />
      <h3>Granular settings</h3>
      Player, gestures, audio, subtitles, appearance, media library, general, and About sections.
    </td>
    <td width="50%" valign="top">
      <img src="docs/assets/dark-player/icons/vault.svg" width="32" height="32" alt="Vault icon" />
      <h3>Hub shortcuts</h3>
      The captured Hub screen surfaces Vault, file picking, stream URL, auto-update, trash, and app settings entries.
    </td>
  </tr>
</table>

## Screenshots

All 16 supplied screenshots are included below and stored under [`docs/assets/dark-player/screenshots`](docs/assets/dark-player/screenshots). Open any image in a new tab for full resolution.

### Library and navigation

<table>
  <tr>
    <td align="center" width="33%"><img src="docs/assets/dark-player/screenshots/dark-player-library-home.png" width="220" alt="Dark Player library home" /><br /><sub>Library home</sub></td>
    <td align="center" width="33%"><img src="docs/assets/dark-player/screenshots/dark-player-library-main.png" width="220" alt="Dark Player main library list" /><br /><sub>Main library</sub></td>
    <td align="center" width="33%"><img src="docs/assets/dark-player/screenshots/dark-player-library-compact.png" width="220" alt="Dark Player compact library view" /><br /><sub>Compact library view</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-library-grid.png" width="220" alt="Dark Player library with bottom navigation" /><br /><sub>Library navigation</sub></td>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-video-list.png" width="220" alt="Dark Player folder video list" /><br /><sub>Folder video list</sub></td>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-hub.png" width="220" alt="Dark Player Hub tools" /><br /><sub>Hub and shortcuts</sub></td>
  </tr>
</table>

### Playback and media controls

<table>
  <tr>
    <td align="center" width="50%"><img src="docs/assets/dark-player/screenshots/dark-player-player-landscape.png" width="430" alt="Dark Player landscape video playback" /><br /><sub>Landscape playback</sub></td>
    <td align="center" width="50%"><img src="docs/assets/dark-player/screenshots/dark-player-player-controls.png" width="430" alt="Dark Player playback controls" /><br /><sub>Playback controls</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-decoder-picker.png" width="430" alt="Dark Player decoder selection overlay" /><br /><sub>Decoder selection</sub></td>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-subtitle-picker.png" width="430" alt="Dark Player subtitle track selection overlay" /><br /><sub>Subtitle selection</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-audio-booster.png" width="430" alt="Dark Player audio booster panel" /><br /><sub>Audio booster panel</sub></td>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-playlists-empty.png" width="220" alt="Dark Player empty playlists screen" /><br /><sub>Playlists empty state</sub></td>
  </tr>
</table>

### Network, settings, and about

<table>
  <tr>
    <td align="center" width="33%"><img src="docs/assets/dark-player/screenshots/dark-player-network-stream.png" width="220" alt="Dark Player network stream setup" /><br /><sub>Network stream</sub></td>
    <td align="center" width="33%"><img src="docs/assets/dark-player/screenshots/dark-player-settings.png" width="220" alt="Dark Player Settings screen" /><br /><sub>Settings</sub></td>
    <td align="center" width="33%"><img src="docs/assets/dark-player/screenshots/dark-player-settings-categories.png" width="220" alt="Dark Player settings categories" /><br /><sub>Settings categories</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/assets/dark-player/screenshots/dark-player-about-links.png" width="220" alt="Dark Player About and ecosystem links" /><br /><sub>About and project links</sub></td>
    <td align="center" colspan="2"><img src="docs/assets/dark-player/dark-player-logo.png" width="220" alt="Dark Player official app icon supplied via IconKitchen" /><br /><sub>Supplied app icon</sub></td>
  </tr>
</table>

## Visual identity

- **Palette:** black and charcoal surfaces with yellow accents, reflecting the supplied UI captures.
- **Iconography:** local SVG line icons for the feature overview and an editable SVG app mark.
- **Motion:** the showcase GIF cycles through actual supplied screenshots with restrained crossfades. It is a README presentation asset, not a claim about in-app animation behavior.
- **App icons:** original IconKitchen output is retained in [`assets/app-icons/icon-kitchen`](assets/app-icons/icon-kitchen), including Android mipmap variants, iOS app icons, and web icons.

## Installation

1. Open the [latest GitHub release](https://github.com/xboyrex/DarkPlayer/releases/latest).
2. Download the Android package attached to the release, if one is published.
3. Install it on a compatible Android device. Android may ask you to approve installation from your selected download source.

If the Releases page has no installable package, use the repository's current build instructions rather than assuming an APK is available.

## Project links

- **Source and releases:** [github.com/xboyrex/DarkPlayer](https://github.com/xboyrex/DarkPlayer)
- **Maintainer profile:** [github.com/xboyrex](https://github.com/xboyrex)
- **Telegram updates:** [t.me/AnmolK2200](https://t.me/AnmolK2200)
- **Mayank Droid:** [mayank-droid.pages.dev](https://mayank-droid.pages.dev)
- **Onyx Services:** [onyx-services.pages.dev](https://onyx-services.pages.dev)
- **Otaku Hub:** [otaku-hub.pages.dev](https://otaku-hub.pages.dev)

For bugs or feature requests, use the repository's [GitHub Issues](https://github.com/xboyrex/DarkPlayer/issues) page.

## Update and publish documentation

The repository includes a guarded Termux publisher for maintainers. After downloading and extracting the documentation pack, run `bash START-TERMUX.sh`. The script backs up the current README and package-owned asset folders, stages only the documentation paths, checks the staged changes, then asks before committing and pushing. It does not alter Android source code or build an APK. See [`README-PUSH-TERMUX.md`](README-PUSH-TERMUX.md) for authentication and recovery details.

## Repository notes

This README pack is a documentation and branding overlay. It does not contain the Android application source code or build outputs. The feature summary is based on the supplied description and the screenshots included with this pack; verify code-level availability against the current app source before treating every visible control as a production-ready feature.

No license terms are asserted here. Follow the repository's `LICENSE` file, if present, before redistributing the app or its assets.

---

<div align="center">
  <sub>Dark Player · Documentation and screenshot refresh</sub>
</div>
