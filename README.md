# ModernUI-GTNH

<p align="center">

<a href="README.zh-CN.md">
<img src="https://img.shields.io/badge/%E8%AF%AD%E8%A8%80-%E4%B8%AD%E6%96%87-red?style=for-the-badge">
</a>

<a href="README.md">
<img src="https://img.shields.io/badge/Language-English-blue?style=for-the-badge">
</a>

</p>

<p align="center">

<a href="https://github.com/nhx100218/ModernUI-GTNH/actions/workflows/build.yml">
<img src="https://github.com/nhx100218/ModernUI-GTNH/actions/workflows/build.yml/badge.svg?branch=main">
</a>

<a href="https://github.com/nhx100218/ModernUI-GTNH/releases">
<img src="https://img.shields.io/github/downloads/nhx100218/ModernUI-GTNH/total?label=Downloads&style=flat">
</a>

<a href="https://github.com/nhx100218/ModernUI-GTNH/releases/">
<img src="https://img.shields.io/github/v/release/nhx100218/ModernUI-GTNH?style=flat">
</a>

<a href="https://raw.githubusercontent.com/nhx100218/ModernUI-GTNH/main/LICENSE">
<img src="https://img.shields.io/github/license/nhx100218/ModernUI-GTNH?style=flat">
</a>

<img src="https://img.shields.io/github/last-commit/nhx100218/ModernUI-GTNH?color=c78aff&label=Last%20Commit&style=flat">

<a href="https://www.curseforge.com/minecraft/mc-mods/modern-ui-gtnh">
<img src="http://cf.way2muchnoise.eu/full_1835100_downloads.svg">
</a>

<a href="https://www.curseforge.com/minecraft/mc-mods/modern-ui-gtnh">
<img src="http://cf.way2muchnoise.eu/versions/For%20Minecraft_1835100_all.svg">
</a>

</p>

A client-side interface and font mod for GTNH on Minecraft 1.7.10. It ports part of Modern UI and adds GTNH-specific integration plus a few extra options.

## What it changes

### Tooltips

Rounded tooltips with adjustable corner radius, border width, and four-corner gradient fill and border colors. They use SDF rendering. Border color can follow item rarity or name color, and it can cycle over time. The tooltips work with NEI, ModularUI2, Tinkers' Construct, Waila, Better Questing, and Chromatic Tooltips.

### Interface and motion

- The config screen uses a three-section layout with a dashboard, clickable project links and a scrollable player changelog. Changes save automatically.
- Range settings use a ModernUI-style slider: a thin outline groove, a filled track, a vertical capsule thumb and an editable value box.
- Config sizes adapt from a 2420x1668 reference resolution, so the page stays usable on other screens without changing the reference layout.
- Screens use non-linear open and close animations.
- Hold C to zoom.
- Vanilla scroll lists support smooth scrolling with real momentum.
- Text selection uses ModernUI colors. Its alignment can still be slightly off.
- Text fields support undo and redo.
- Container pause is available in the interface settings. It is off by default.

### Chat

- Chat fades in when it opens.
- Player skin heads appear next to chat messages.
- Emoji shortcodes such as `:name:` are converted when a system emoji font is available.

### Font rendering

- Replaces the vanilla font renderer with a glyph pipeline that supports modern font files.
- Ships Inter and Source Han Sans and loads them directly from the mod jar. The font stack accepts family names or file paths and selects bold/italic faces automatically.
- Bundled bold faces were removed. Bold text is synthesized from the regular face, which keeps the download much smaller.
- Glyph generation resolution and character size are adjustable. High values can freeze the game.

### Performance

The mod lowers the frame rate and volume when the Minecraft window is not focused. Some GTNH mods already do the same, so the effects can overlap.

### Server MOTD

- If MiniMOTD is installed, its `config/MiniMOTD/main.conf` is used directly.
- 1.7.10 clients receive the normal 16-colour MOTD. 1.16+ clients receive exact RGB colours, so gradients stay smooth.
- MOTD and TPS switches are no longer part of the client config screen; MiniMOTD's own config is authoritative.

### Recent fixes

- Forge startup warning screens are no longer hidden by the loading screen.
- The key-binding list keeps mouse-wheel scrolling after a search.
- The in-game changelog follows the game language, wraps to the card width and can be scrolled.
- The copyright line stays at the bottom of the home page.

## Requirements

| Type | Version |
|------|---------|
| Minecraft | 1.7.10 (GTNH) |
| Side | Client |

## Installation

1. Download the latest `modernui-gtnh-*.jar` from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui-gtnh) or [GitHub Releases](https://github.com/nhx100218/ModernUI-GTNH/releases).
2. Put it in the GTNH modpack's `mods/` folder.
3. Launch the game. Bundled fonts are loaded directly from the jar; no font files are extracted.

## Configuration

- The config file is `config/modernui.yaml`.
- Open the config page from the mod list's **Config** button or with **Ctrl+K**.
- Changes save automatically. The page covers both interface enhancements and font rendering options.
- The home page includes a player-facing changelog. The full development history is in `CHANGELOG.md`.

## Building from source

Requires JDK 21 or newer. JDK 25 or newer is recommended.

```bash
./gradlew build
```

## Authors

- Original author: BloCamLimb
- GTNH port and merge: nhx100218
- Qz-UILib as overall reference: QuanhuZeYu

## Acknowledgements

Thanks to BloCamLimb for the original ModernUI project, and to the GTNH modding toolchain.

Thanks to QuanhuZeYu for Qz-UILib, which provided the foundation for this project's implementation.

## License

This project is open-sourced under [LGPL-3.0](LICENSE). Forks must stay open source too.

## CI

GitHub Actions builds the mod and publishes only the main mod jar, so nobody grabs the wrong file. Publishing a GitHub Release also attaches the jar and uploads the same file to CurseForge with the player-facing changelog.
