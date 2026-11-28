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

<a href="https://www.curseforge.com/minecraft/mc-mods/modern-ui-gtnh">
<img src="http://cf.way2muchnoise.eu/full_1835100_downloads.svg">
</a>

<a href="https://www.curseforge.com/minecraft/mc-mods/modern-ui-gtnh">
<img src="http://cf.way2muchnoise.eu/versions/For%20Minecraft_1835100_all.svg">
</a>

</p>

面向 GTNH（Minecraft 1.7.10）的客户端界面与字体 mod。项目移植了 Modern UI 的部分功能，并加入 GTNH 适配和一些额外选项。

## 改动内容

### 提示框

圆角提示框支持调整圆角半径、边框宽度、四角渐变填充和边框颜色，使用 SDF 渲染。边框颜色可跟随物品稀有度或名称颜色，也可以随时间循环。可与 NEI、ModularUI2、Tinkers' Construct、Waila、Better Questing 和 Chromatic Tooltips 配合。

### 界面与动画

- 配置界面采用主页 + 三个分区的简洁布局，包含可点击的项目链接和可滚动的玩家更新日志；修改会自动保存。
- 带范围的设置项使用 ModernUI 风格滑条：细凹槽、填充轨道、竖直胶囊滑块，右侧可以直接输入数值。
- 配置界面按 2420x1668 的参考分辨率等比适配，在其他屏幕上也能正常使用，同时不改变参考分辨率下的原布局。
- 界面打开和关闭使用非线性动画。
- 按住 C 缩放。
- 原版滚动列表支持带惯性的平滑滚动。
- 文本选择框使用 ModernUI 配色，位置仍可能略有偏移。
- 文本框支持撤销和重做。
- 界面设置里可以开启容器暂停，默认关闭。

### 聊天

- 聊天栏打开时有淡入动画。
- 聊天消息旁显示玩家皮肤头像。
- 系统安装了 emoji 字体时，`:name:` 这类短代码会转换为 emoji。

### 字体渲染

- 使用现代字形管线替换原版字体渲染，支持现代字体文件。
- 内置 Inter 与思源黑体，直接从模组 jar 加载。字体家族栈支持填写家族名或字体文件路径，并自动选择粗体和斜体。
- 不再内置粗体字体文件，粗体由常规字面自动合成，下载体积更小。
- 字形生成分辨率和游戏字符大小可以调整。数值太高可能导致游戏卡死。

### 性能

Minecraft 窗口失去焦点时，mod 会降低帧率和音量。部分 GTNH mod 也有类似功能，效果可能叠加。

### 服务端 MOTD

- 安装 MiniMOTD 时，直接使用它的 `config/MiniMOTD/main.conf`。
- 1.7.10 客户端收到正常的 16 色 MOTD；1.16+ 客户端收到精确 RGB 颜色，渐变会更平滑。
- 客户端设置页不再包含 MOTD/TPS 开关，MiniMOTD 自己的配置为准。

### 近期修复

- Forge 启动时的警告/询问界面不再被加载画面遮住。
- 按键列表在搜索过滤后仍可用滚轮滚动。
- 配置界面的更新日志会跟随游戏语言、按卡片宽度换行，并可以滚动。
- 版权信息固定显示在主页底部。

## 运行要求

| 类型 | 版本 |
|------|------|
| Minecraft | 1.7.10（GTNH） |
| 运行侧 | 客户端 |

## 安装

1. 从 [CurseForge](https://www.curseforge.com/minecraft/mc-mods/modern-ui-gtnh) 或 [Releases](https://github.com/nhx100218/ModernUI-GTNH/releases) 下载最新的 `modernui-gtnh-*.jar`。
2. 放入 GTNH 整合包的 `mods/` 目录。
3. 启动游戏。内置字体直接从 jar 加载，不再释放到 fonts 目录。

## 配置

- 配置文件位于 `config/modernui.yaml`。
- 通过 mod 列表的 **Config** 按钮或 **Ctrl+K** 打开配置页。
- 修改会自动保存。配置页同时包含界面增强和字体渲染选项。
- 主页包含面向玩家的更新日志；完整开发记录在 `CHANGELOG.md`。

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

GitHub Actions 会构建 mod，并且只发布主 mod jar，避免下载错文件。推送 `v2.0.2` 或 `2.0.2` 这样的发布 tag 时，工作流会自动构建、创建 GitHub Release、附加 jar，并用面向玩家的更新日志把同一个文件上传到 CurseForge。
