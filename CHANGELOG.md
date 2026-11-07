# CHANGELOG

本项目采用 [Keep a Changelog](https://keepachangelog.com/) 风格记录变更，版本号遵循
`主.次.修订[-标签]` 格式：主版本号变更代表破坏性 API 调整，次版本号代表能力扩展，
修订号代表行为修复或文档调整。

## [Unreleased]

## [1.2.1] - 2026-11-07

### English

#### Changed

- ModernUI-GTNH no longer requires UniMixins, GTNHLib, or ModularUI2 at runtime. The jar embeds a Mixin/MixinExtras runtime and prefers a provider already installed in `mods`; the embedded copy is used only when none is found.
- The jar is coremod-only again. The embedded runtime is installed during coremod discovery, before any Mixin tweaker is constructed, and the early and late configs are registered before Mixin's first DEFAULT-phase selection.
- GTNHLib and ModularUI2 integration remains optional.
- New installs use the GTNH 2.9.0 font assignments by default: `Inter Bold` for western bold, `Inter Frozen Medium Italic` for western italic, and `Source Han Sans SC Bold` for CJK bold.

#### Added

- `/mug` server command (permission level 2) with `status`, `reload`, `tab` and `motd` subcommands. Changes take effect immediately and are written back to `config/modernui.yaml` where applicable.
- The `server` section in `modernui.yaml` holds the tab toggle and fallback MOTD settings. Server features start automatically on dedicated and integrated servers.
- TPS/MSPT sampling with `{tps}`, `{mspt}`, `{players}`, `{maxplayers}`, `{memory}`, `{cpu}` and `{uptime}` placeholders.
- Server-list MOTD with MiniMOTD-style `<gradient:...>`, `<rainbow>`, named and hex colours, and a nearest legacy-colour fallback; optional 64x64 PNG icon. ModernUI clients rebuild the legacy colour runs into a smooth per-glyph gradient. If the gradient cannot be drawn, the legacy fallback is shown. Angelica's SDL GPU backend keeps the fallback.
- MiniMOTD config interop: `config/MiniMOTD/main.conf` and `config/MiniMOTD/icons/` are used as-is when present and take precedence over `modernui.yaml`. `/mug motd on|off` overrides the state until restart.
- ServerUtilities tab integration: one `MSPT/TPS` line is appended to ServerUtilities' existing footer, leaving its header and custom footer content intact. The feature is disabled when ServerUtilities is absent.

#### Fixed

- TPS is sampled over the elapsed 20-tick window instead of a single tick, so values such as 380 no longer appear and the result is capped at 20.
- MiniMOTD lines that render to an empty string fall back to the `modernui.yaml`/default MOTD instead of leaving a blank server-list entry.
- The gradient MOTD advances by glyph width instead of the absolute end-x returned by `FontRenderer.drawString`, so the text stays inside the server-list entry.
- The client gradient falls back to plain legacy text when parsing fails, no glyphs are produced, or the active backend cannot draw per-glyph colours.
- The late compatibility config is registered before Mixin's first DEFAULT-phase selection. Waila, ChromaticTooltips, ModularUI2 and other late tooltip mixins load again.
- Mixin classes from the embedded runtime and from an installed provider stay on one class loader, so config plugins no longer hit loader constraint errors.
- The blanket `org.spongepowered.asm.*` class-loader exclusion was removed. Mixin generates synthetic classes such as `Args$N` in the launch class loader, and the exclusion sent those lookups to the parent, crashing `@Mod` construction for mods that use `@ModifyArgs`.

#### Removed

- The BetterQuesting text-input bridge and its mixins were removed; current BetterQuesting versions handle input themselves.

Compatibility: 1.2.1 accepts remote versions `[1.2.0,1.3.0)`.

### 中文

#### 变更

- ModernUI-GTNH 运行时不再强制依赖 UniMixins、GTNHLib 或 ModularUI2。jar 内嵌 Mixin/MixinExtras 运行时，并优先使用 `mods` 中已安装的 provider；只有找不到时才使用内置副本。
- jar 恢复为纯 coremod。内置运行时在 coremod 发现阶段安装，早于任何 Mixin tweaker 的构造；early 与 late 配置都在 Mixin 首次 DEFAULT 阶段选择前注册。
- GTNHLib 与 ModularUI2 的集成保持可选。
- 新安装默认使用 GTNH 2.9.0 的字体指派：西文粗体 `Inter Bold`、西文斜体 `Inter Frozen Medium Italic`、CJK 粗体 `Source Han Sans SC Bold`。

#### 新增

- `/mug` 服务端指令（权限等级 2），提供 `status`、`reload`、`tab`、`motd` 子命令。修改立即生效，并在适用时写回 `config/modernui.yaml`。
- `modernui.yaml` 新增 `server` 段，包含 tab 开关和 MOTD 回退配置。专用服务端与内置服务端都会自动启动对应功能。
- TPS/MSPT 采样，支持 `{tps}`、`{mspt}`、`{players}`、`{maxplayers}`、`{memory}`、`{cpu}`、`{uptime}` 占位符。
- 服务器列表 MOTD 支持 MiniMOTD 风格的 `<gradient:...>`、`<rainbow>`、命名色和 HEX 色，并回退到最近的 16 种原版颜色；可选 64x64 PNG 图标。ModernUI 客户端会把 legacy 色段重建为平滑的逐字形渐变；无法绘制渐变时显示 legacy 回退。Angelica 的 SDL GPU 后端保留回退效果。
- 直接使用 MiniMOTD 配置：存在 `config/MiniMOTD/main.conf` 与 `config/MiniMOTD/icons/` 时原样使用，并优先于 `modernui.yaml`。`/mug motd on|off` 作为重启前生效的运行时开关。
- 与 ServerUtilities 的 tab 联动：只在 ServerUtilities 现有 footer 末尾追加一行 `MSPT/TPS`，保留其 header 和自定义 footer 内容；未安装 ServerUtilities 时不启用。

#### 修复

- TPS 改为按实际经过的 20 tick 窗口采样，不再使用单 tick 耗时；不会再出现 380 之类的数值，并封顶 20。
- MiniMOTD 行渲染为空时回退到 `modernui.yaml`/默认 MOTD，不再留下空的服务器列表条目。
- 渐变 MOTD 按字形宽度前进，而不是把 `FontRenderer.drawString` 返回的绝对结束 x 当作前进量，文本不会再被推出条目外。
- 客户端渐变在解析失败、没有字形或当前后端不能绘制逐字形颜色时，回退为纯 legacy 文本。
- late 兼容配置在 Mixin 首次 DEFAULT 阶段选择前注册；Waila、ChromaticTooltips、ModularUI2 等 tooltip mixin 恢复加载。
- 内置运行时与已安装 provider 的 Mixin 类固定在同一类加载器上，配置插件不再出现 loader constraint 错误。
- 移除了 `org.spongepowered.asm.*` 的全局类加载排除。Mixin 会在启动类加载器中生成 `Args$N` 等合成类，排除规则会让这些查找落到父加载器，导致使用 `@ModifyArgs` 的模组在构造 `@Mod` 实例时崩溃。

#### 移除

- 移除 BetterQuesting 文本输入桥及其 mixin；当前版本的 BQ 已自带输入处理。

兼容性：1.2.1 接受远端版本 `[1.2.0,1.3.0)`。


## [1.2.0] - 2026-11-07

### English

#### Added

- ModernUI now runs on Angelica's SDL GPU backend (Vulkan and Direct3D 12). Font glyphs on that backend are submitted through the host fixed pipeline, with alpha contrast baked into the glyph atlas at upload time. Custom fonts, font metrics, and colored glyphs still come from ModernUI.
- When custom font replacement is enabled, the Angelica 2.2.44 F3 screen measures and draws its text with the ModernUI font.
- NEI recipe panels can be shifted with `tooltip.neiPanelOffsetX` and `tooltip.neiPanelOffsetY`. Both settings have English and Chinese labels.
- Zoom now scales lwjgl3ify's float mouse deltas through reflection. The mod loads normally when the API is absent.
- Angelica 2.3+ exposes `attribDepth` through `GLContextState`; the GL depth guard reads the new location.

#### Changed

- On the SDL GPU backend, a full-screen ModernUI screen skips world rendering behind it, disables FBO compositor layers and full-screen snapshots, clears the whole framebuffer before drawing, and uses rectangular clipping instead of stencil masks. Rounded corners, backdrop blur, and HUD frames while a screen is open are therefore unavailable on this backend.
- Filled rounded rectangles use plain quads on SDL GPU instead of `GL_TRIANGLE_FAN`.
- Ordinary host images such as the config navigation icons are drawn as direct textured quads on SDL GPU.
- Tooltip SDF shaders fall back to the CPU rounded path on Direct3D 12 because the backend rejects the custom pipeline.
- The GTNHLib and ModularUI2 tooltip listeners load only when those mods are present.
- Mixin target validation is relaxed for classes that exist only in Angelica 2.2.44. Runtime injection still uses `require = 0`.

#### Fixed

- The config screen no longer hangs the GPU or shows a flickering color block, radiating beams, or stretched textures on Vulkan and Direct3D 12.
- Navigation icons in the config screen are visible again on SDL GPU.
- Empty clip rectangles no longer write the 1x1 fallback area or clear stencil on the D3D12 backend.
- The tooltip shader no longer inherits the previous vertex color.
- The renderbuffer binding query is skipped on SDL GPU because the backend does not implement it.

Compatibility: 1.2.0 accepts remote versions `[1.2.0,1.3.0)`. Upgrade the client and server together from 1.1.x.

### 中文

#### 新增

- ModernUI 现在可以在 Angelica 的 SDL GPU 后端（Vulkan 和 Direct3D 12）下运行。该后端的字形改由宿主固定管线提交，alpha 对比度在纹理上传时预先烘焙进字形图集。自定义字体、字形度量和彩色字形仍由 ModernUI 提供。
- 开启自定义字体替换后，Angelica 2.2.44 的 F3 界面改用 ModernUI 字体测量和绘制文本。
- 新增 `tooltip.neiPanelOffsetX` 和 `tooltip.neiPanelOffsetY`，可以调整 NEI 合成面板的位置，并提供中英文标签。
- 缩放时会通过反射调整 lwjgl3ify 的浮点鼠标增量；缺少该 API 时模组仍可正常加载。
- Angelica 2.3+ 把 `attribDepth` 移到了 `GLContextState`，GL 深度守卫已能读取新位置。

#### 变更

- SDL GPU 后端下，ModernUI 全屏界面打开时跳过其后的世界渲染，关闭 FBO 合成图层和全屏快照，绘制前清空整个帧缓冲，圆角裁剪退化为矩形裁剪。因此该后端暂时没有圆角、背景模糊和界面打开时的 HUD。
- SDL GPU 下，填充圆角矩形改用普通四边形绘制，不再提交 `GL_TRIANGLE_FAN`。
- SDL GPU 下，配置界面的导航图标等普通 HostImage 改为直接绘制纹理四边形。
- Direct3D 12 后端拒绝自定义 SDF 管线，tooltip 圆角改用 CPU 路径。
- GTNHLib 和 ModularUI2 的 tooltip 监听器只在对应模组存在时加载。
- 对仅存在于 Angelica 2.2.44 的类放宽 Mixin target 校验，运行时注入仍使用 `require = 0`。

#### 修复

- 配置界面在 Vulkan 和 Direct3D 12 下不再挂死 GPU，也不再出现闪烁色块、放射光束或拉伸纹理。
- SDL GPU 下配置界面的导航图标恢复显示。
- 空裁剪矩形不再写入 1x1 兜底区域，也不会在 D3D12 后端清理 stencil。
- tooltip 着色器不再继承上一笔顶点颜色。
- SDL GPU 不实现 renderbuffer 绑定查询，现已跳过该查询。

兼容性：1.2.0 接受远端版本 `[1.2.0,1.3.0)`。从 1.1.x 升级时，客户端和服务端需要一起更新。

