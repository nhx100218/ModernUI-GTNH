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

</p>

A client-side interface and font mod for GTNH on Minecraft 1.7.10. It ports part of Modern UI and adds GTNH-specific integration plus a few extra options.

## What it changes

### Tooltips

Rounded tooltips with adjustable corner radius, border width, and four-corner gradient fill and border colors. They use SDF rendering. Border color can follow item rarity or name color, and it can cycle over time. The tooltips work with NEI, ModularUI2, Tinkers' Construct, Waila, Better Questing, and Chromatic Tooltips.

### Interface and motion

- The config screen uses a Material 3 layout with solid panels, section navigation, page transitions, and automatic saving.
- Screens use non-linear open and close animations.
- Hold C to zoom.
- Vanilla scroll lists support smooth scrolling.
- Text selection uses ModernUI colors. Its alignment can still be slightly off.
- Text fields support undo and redo.
- Container pause is available in the interface settings. It is off by default.

### Chat

- Chat fades in when it opens.
- Player skin heads appear next to chat messages.
- Emoji shortcodes such as `:name:` are converted when a system emoji font is available.

### Font rendering

- Replaces the vanilla font renderer with a glyph pipeline that supports modern font files.
- Ships Inter Frozen and Source Han Sans for players coming from newer Minecraft versions.
- Glyph generation resolution and character size are adjustable. High values can freeze the game.

### Performance

The mod lowers the frame rate and volume when the Minecraft window is not focused. Some GTNH mods already do the same, so the effects can overlap.

## Requirements

| Type | Version |
|------|---------|
| Minecraft | 1.7.10 (GTNH) |
| Side | Client |

## Installation

1. Download the latest `modernui-gtnh-*.jar` from [Releases](https://github.com/nhx100218/ModernUI-GTNH/releases).
2. Put it in the GTNH modpack's `mods/` folder.
3. Launch the game. The bundled fonts are released on first launch and may cause a short stutter.

## Configuration

- The config file is `config/modernui.yaml`.
- Open the config page from the mod list's **Config** button or with **Ctrl+K**.
- Changes save automatically. The page covers both interface enhancements and font rendering options.
- The layout is not identical to upstream ModernUI in every detail.

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

GitHub Actions builds the mod and publishes only the main mod jar, so nobody grabs the wrong file.
