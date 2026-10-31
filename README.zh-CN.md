# ModernUI-GTNH

<p align="center">

<a href="README.md">
<img src="https://img.shields.io/badge/Language-English-blue?style=for-the-badge">
</a>

<a href="README.zh-CN.md">
<img src="https://img.shields.io/badge/%E8%AF%AD%E8%A8%80-%E4%B8%AD%E6%96%87-red?style=for-the-badge">
</a>

</p>

<p align="center">

<a href="https://github.com/nhx100218/ModernUI-GTNH/actions/workflows/build.yml">
<img src="https://github.com/nhx100218/ModernUI-GTNH/actions/workflows/build.yml/badge.svg?branch=main">
</a>

<a href="https://github.com/nhx100218/ModernUI-GTNH/releases">
<img src="https://img.shields.io/github/downloads/nhx100218/ModernUI-GTNH/total?label=%E4%B8%8B%E8%BD%BD&style=flat">
</a>

<a href="https://github.com/nhx100218/ModernUI-GTNH/releases/">
<img src="https://img.shields.io/github/v/release/nhx100218/ModernUI-GTNH?style=flat">
</a>

<a href="https://raw.githubusercontent.com/nhx100218/ModernUI-GTNH/main/LICENSE">
<img src="https://img.shields.io/github/license/nhx100218/ModernUI-GTNH?style=flat">
</a>

<img src="https://img.shields.io/github/last-commit/nhx100218/ModernUI-GTNH?color=c78aff&label=%E6%9C%80%E8%BF%91%E6%8F%90%E4%BA%A4&style=flat">

</p>

面向 GTNH（Minecraft 1.7.10）的客户端界面与字体 mod。项目移植了 Modern UI 的部分功能，并加入 GTNH 适配和一些额外选项。

## 改动内容

### 提示框

圆角提示框支持调整圆角半径、边框宽度、四角渐变填充和边框颜色，使用 SDF 渲染。边框颜色可跟随物品稀有度或名称颜色，也可以随时间循环。可与 NEI、ModularUI2、Tinkers' Construct、Waila、Better Questing 和 Chromatic Tooltips 配合。

### 界面与动画

- 配置界面采用 Material 3 布局，包含纯色面板、分区导航、页面切换动画和自动保存。
- 界面打开和关闭使用非线性动画。
- 按住 C 缩放。
- 原版滚动列表支持平滑滚动。
- 文本选择框使用 ModernUI 配色，位置仍可能略有偏移。
- 文本框支持撤销和重做。
- 界面设置里可以开启容器暂停，默认关闭。

### 聊天

- 聊天栏打开时有淡入动画。
- 聊天消息旁显示玩家皮肤头像。
- 系统安装了 emoji 字体时，`:name:` 这类短代码会转换为 emoji。

### 字体渲染

- 使用现代字形管线替换原版字体渲染，支持现代字体文件。
- 内置 Inter Frozen 和思源黑体，方便从高版本 Minecraft 转过来的玩家。
- 字形生成分辨率和游戏字符大小可以调整。数值太高可能导致游戏卡死。

### 性能

Minecraft 窗口失去焦点时，mod 会降低帧率和音量。部分 GTNH mod 也有类似功能，效果可能叠加。

## 运行要求

| 类型 | 版本 |
|------|------|
| Minecraft | 1.7.10（GTNH） |
| 运行侧 | 客户端 |

## 安装

1. 从 [Releases](https://github.com/nhx100218/ModernUI-GTNH/releases) 下载最新的 `modernui-gtnh-*.jar`。
2. 放入 GTNH 整合包的 `mods/` 目录。
3. 启动游戏。首次启动会释放内置字体，可能出现短暂卡顿。

## 配置

- 配置文件位于 `config/modernui.yaml`。
- 通过 mod 列表的 **Config** 按钮或 **Ctrl+K** 打开配置页。
- 修改会自动保存。配置页同时包含界面增强和字体渲染选项。
- 页面布局与上游 ModernUI 并非完全一致。

## 从源码构建

需要 JDK 21 或更高版本，推荐 JDK 25+。

```bash
./gradlew build
```

## 作者

- 原作者：BloCamLimb
- GTNH 移植与合并：nhx100218
- 整体参考：QuanhuZeYu 的 Qz-UILib

## 致谢

感谢 BloCamLimb 的 ModernUI 原始项目，以及 GTNH mod 工具链。

感谢 QuanhuZeYu 的 Qz-UILib，它为本项目的实现提供了基础。

## 许可证

本项目基于 [LGPL-3.0](LICENSE) 开源。Fork 也需要保持开源。

## CI

GitHub Actions 会构建 mod，并且只发布主 mod jar，避免下载错文件。
