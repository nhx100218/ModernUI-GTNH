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

A client-side interface beautification and font rendering mod for **GTNH** (Minecraft 1.7.10). It's a partial port of Modern UI to 1.7.10.

## Highlights

- **Interface and motion**: made with reference to Zoomify and SmoothGUI.
- **ToolTips**: restores the original Modern UI look.
- **Modern font rendering**: a more modern font rendering engine, made with reference to [Qz-UILib](https://github.com/QuanhuZeYu/Qz-UILib).
- **Bundled fonts**: ships Inter Frozen and Source Han Sans with the mod, to help players coming from newer versions.
- **Config screen restoration**: I'm working on recreating the original MD3 config screen. Stay tuned.

## Features

### Tooltips
- Rounded, gradient tooltips with configurable corner radius, border width, and four-corner gradient fill / border colors. Fully SDF-rendered.

*Adaptive border color (follows item rarity / name color), with optional animated border color cycling.*
- Works with **NEI**, **ModularUI2**, **Tinkers' Construct**, **Waila**, **Better Questing**, and **Chromatic Tooltips**.


### Interface and motion
- Non-linear open / close transition animations.
- Hold C to zoom.
- Smooth scrolling for vanilla scroll lists.
- Custom text selection highlight (*unfortunately it's a bit offset*).
- Text field undo / redo.
- Pauses the world when opening a screen in singleplayer (*honestly I don't know what this is good for*).

### Chat
- Chat fade-in animation.
- Sender skin heads in chat.
- Emoji shortcode conversion (`:name:` to emoji; requires a system emoji font).

### Font rendering
- Replaces vanilla font rendering with a modern glyph pipeline.
- Glyph generation resolution and game character size are adjustable. Don't set them too high or your game will freeze.

### Performance
- Lowers framerate and volume when the window is unfocused. This overlaps with quite a few GTNH mods, though.

## Requirements

| Type | Version |
|------|---------|
| Minecraft | 1.7.10 (GTNH) |
| Side | Client |


## Installation

1. Download the latest `modernui-gtnh-*.jar` from [Releases](https://github.com/nhx100218/ModernUI-GTNH/releases).
2. Put it into the GTNH modpack's `mods/` folder.
3. Launch the game. The bundled fonts are released automatically on first launch, with a little stutter.

## Configuration

- Config file: `config/modernui.yaml`.
- Open the config page from the mod list's **Config** button or with **Ctrl+K**.
- The page has both the interface enhancement options and the font rendering options **(currently different from ModernUI, keep that in mind)**.

## Building from source

Requires JDK 21 or newer. JDK 25+ recommended.

```bash
./gradlew build
```


## Authors

- Original author: BloCamLimb
- GTNH port and merge: nhx100218
- Qz-UILib as overall reference: QuanhuZeYu

## Acknowledgements

Thanks to BloCamLimb for the original ModernUI project, and the GTNH modding toolchain.
Thanks to QuanhuZeYu for Qz-UILib, the foundation of this project's implementation.

## License

This project is open-sourced under [LGPL-3.0](LICENSE). Forks must stay open source too.

## CI

GitHub Actions builds the mod and publishes only the main mod jar, so nobody grabs the wrong file.
