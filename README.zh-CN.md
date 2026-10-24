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

面向 **GTNH**（Minecraft 1.7.10）的客户端界面美化与字体渲染mod。作为Modern UI mod在1.7.10的部分移植。

## 主要特性

- **界面与动效优化**：参考Zoomfiy和SommthGUI mod制作。
- **ToolTip**：还原原版Modern UI。
- **现代字体渲染**：更加现代化的字体渲染引擎，参考自[Qz-UILib](https://github.com/QuanhuZeYu/Qz-UILib)制作。
- **内置字体**：随mod打包 Inter Frozen 与思源黑体，为从高版本下来的玩家提供适应。
- **配置界面还原**：我正在尝试制作原版的MD3配置界面，请期待。

## 功能

### 提示框（Tooltip）
- 圆角、渐变提示框，可配置圆角半径、边框宽度和四角渐变填充 / 边框颜色，SDF渲染完全使用。

*自适应边框颜色（跟随物品稀有度 / 名称颜色），可选边框颜色循环动画。*
- 适配 **NEI**、**ModularUI2**、**匠魂**、**Waila**、**Better Questing** 和 **Chromatic Tooltips**。


### 界面与动效
- 界面打开 / 关闭过渡的非线性动画。
- 按C键住缩放。
- 原版滚动列表的平滑滚动。
- 自定义文本选择高亮（*可惜有点小小的偏移*）。
- 文本框撤销 / 重做的功能。
- 单人游戏中打开界面时暂停世界（*其实我不知道这玩意有啥用*）。

### 聊天
- 聊天框淡入动画。
- 聊天消息显示发送者皮肤的头像。
- Emoji 短代码转换（`:name:` 转为 emoji，需要系统 emoji 字体）。

### 字体渲染
- 用自现代化字形管线替换原版字体渲染。
- 字形生成分辨率与游戏字符大小可调，别调太高卡死你。

### 性能
- 窗口失焦时自动降低帧率与音量，不过与GTNH的不少mod重合了。

## 环境要求

| 类型 | 版本 |
|------|------|
| Minecraft | 1.7.10（GTNH） |
| 运行侧 | 客户端 |


## 安装

1. 从 [Releases](https://github.com/nhx100218/ModernUI-GTNH/releases) 下载最新的 `modernui-gtnh-*.jar`。
2. 放入 GTNH 整合包的 `mods/` 目录。
3. 启动游戏。首次启动会自动释放内置字体，略有卡顿。

## 配置

- 配置文件：`config/modernui.yaml`。
- 通过mod列表的 **Config** 按钮或 **Ctrl+K** 打开配置页。
- 配置页同时包含界面增强选项与字体渲染选项 **（与ModernUI暂时不同，需要注意）**。

## 从源码构建

需要 JDK 21 或更高版本。推荐 JDK 25+。

```bash
./gradlew build
```


## 作者

- 原作者：BloCamLimb
- GTNH 移植与合并：nhx100218
- Qz-UILib作为整体参考：QuanhuZeYu

## 致谢

感谢 BloCamLimb 的 ModernUI 原始项目，以及 GTNH mod开发工具链。
感谢 QuanhuZeYu 的 Qz-UILib，作为项目的实现基本。

## 许可证

本项目基于 [LGPL-3.0](LICENSE) 开源，再次fork需要接着开源。

## CI 产物

GitHub Action 会构建mod并发布唯一的主mod jar，防止有些笨蛋下错。
