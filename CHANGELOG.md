# CHANGELOG

## [2.0.1] - 2026-11-22

### English

#### Added

- A ModernUI-style font family stack: two editable rows for the primary and fallback families, accepting either a family name or a font file path. Bundled Inter and Source Han Sans are loaded directly from the mod jar; no font files are extracted to `fonts/`.
- Automatic bold and italic face selection from the active family. A real Italic face is used when the family ships one; the synthetic slant is only a fallback for families without an italic face.
- Alias resolution for localized and English family names (`思源黑体`, `Source Han Sans CN Medium`, `Inter Frozen Medium`), so existing configs keep working without renaming the font.

#### Changed

- Font selection moved from Advanced to the Interface group. The old drag-sorted font list and the six-slot face assignment UI are replaced by the two-row family stack.
- Top sub-tabs received a tonal background and a selected-state surface, so they stay readable over the transparent page shell.

#### Fixed

- MiniMOTD gradients keep exact RGB on 26.3 and now also on 1.7.10 ModernUI clients. Per-glyph colours travel in a separate `modernui_rgb` status field, while the legacy description stays plain 16-colour text, so the 1.7.10 server-list ping no longer stalls.
- Bold and italic decorations survive gradient colour changes in both the RGB component and the legacy fallback.
- Forge's `modinfo` is preserved in the custom status JSON, so the multiplayer list reports FML/Forge instead of "Vanilla".
- The transparent panorama now covers `GuiConnecting` (server login), `GuiDownloadTerrain`, `GuiDisconnected`, and EFR's `GuiLoadingBridge`. EFR's SRG-named bridge method is intercepted correctly and no longer draws the dirt background.
- The Add Server screen joins the shared panorama.

#### Notes

- Run the same 2.0.1 jar on the server and client for exact MOTD colours and FML server-type detection.
- Existing `modernui.yaml` files keep working; `fontSort` still accepts the old English family names.

Compatibility: 2.0.1 accepts remote versions `[2.0.0,2.1.0)`.

### 中文

#### 新增

- ModernUI 风格字体家族栈：主字体与回退字体两行可编辑文本框，支持字体家族名或字体文件路径。内置 Inter 与思源黑体直接从模组 jar 加载，不再释放到 `fonts/` 目录。
- 自动选择粗体/斜体字面：家族有真实 Italic 时使用真实字面，没有时才合成斜体。
- 家族别名兼容本地化与英文名称（`思源黑体`、`Source Han Sans CN Medium`、`Inter Frozen Medium`），旧配置无需手动改字体名。

#### 变更

- 字体选择从「高级」移到「界面」分组；删除旧的自动排序列表和六槽字面指派界面，改为两行字体家族栈。
- 顶部子标签恢复 tonal 背景和选中态表面，透明页壳下仍清晰可读。

#### 修复

- MiniMOTD 渐变在 26.3 和 1.7.10 ModernUI 客户端上保持精确 RGB。逐字颜色改由独立的 `modernui_rgb` 状态字段传输，legacy description 保持纯十六色，1.7.10 服务器列表不再卡在「正在 ping」。
- 渐变换色时粗体/斜体不再丢失，RGB 组件与 legacy 回退都保留样式。
- 自定义 status JSON 保留 Forge 的 `modinfo`，多人列表重新显示 FML/Forge，而不是「原版服务器」。
- 透明全景覆盖 `GuiConnecting`（服务器登录）、`GuiDownloadTerrain`、`GuiDisconnected` 和 EFR 的 `GuiLoadingBridge`；EFR 的 SRG 方法名已正确拦截，不再绘制泥土背景。
- 添加服务器界面加入共享全景。

#### 说明

- 服务端与客户端建议使用同一个 2.0.1 jar，以获得精确 MOTD 颜色和 FML 服务器类型识别。
- 现有 `modernui.yaml` 继续可用；`fontSort` 仍接受旧英文家族名。

兼容性：2.0.1 接受远端版本 `[2.0.0,2.1.0)`。

## [2.0.0] - 2026-11-22

### English

#### Added

- Modern menu overhaul ported from Minecraft 26.3: a shared translucent panorama with matching camera projection, 2px list separators, a roughly 30% black scrim, opaque selection frames and clean row clipping. Main-menu screens keep the panorama; in-world screens use a transparent world mask instead.
- A 26.3 world loading screen with the centred mark, title, thin progress bar and chunk grid. Nether and End loads use their matching portal backdrops; resource-reload and world-save status boxes are drawn inside the screen and presented once to avoid flicker.
- Reworked singleplayer and multiplayer lists. World rows show a join arrow on the icon; server rows show a join arrow on the icon and up/down arrows on the left quarters, with the new order written back to `servers.dat`. Both arrow sets reuse `textures/gui/resource_packs.png`, so no new GUI art is shipped.
- 26.3 cursor mapping for vanilla screens: pointer for buttons, clickable rows and sliders; I-beam for text fields; EW-resize for sliders. World and server rows only show the hand over the icon, and all screens share one cursor cache with ModernUI scenes.
- Smooth scrolling for lists and text fields, configurable through `smoothscroll.listAmount` (default 40 px).
- White search-field borders matching the 26.3 list treatment.
- Optional Et Futurum Requiem integration: when EFR is installed, ModernUI takes over its loading screen; when it is absent, the vanilla loading path is used.
- 26.3-style command suggestions in vanilla chat. The popup uses 10 rows of 12 px, a `0xD0000000` panel, white dashed scroll edges, a yellow selected row and grey other rows. It is anchored to the completed word and clamped to the input width.
- Tab and Shift+Tab cycle the suggestions with automatic scrolling; the mouse can hover and click a row, the wheel scrolls the list, and the cursor becomes a hand over the popup. Vanilla's comma-separated candidate chat line is suppressed.
- Chat input colours now follow 26.3: grey command literals, aqua/yellow/green/light-purple/gold arguments, yellow for the word being completed, and red for unknown players, items or game modes.
- Suggestions combine client commands with a cached server command list, online players, item and block ids, game modes, and server-provided arguments such as `gamerule` rules, `time`/`weather` values and `scoreboard` subcommands. `/gamemode` offers `spectator` when Et Futurum Requiem is installed.
- Server requests are processed in send order and applied by the popup without rewriting the input. Tab uses the local list when possible, avoiding full item-id round trips, and large candidate sets are capped to keep `/give` responsive.

#### Changed

- The remote compatibility range is now `[2.0.0,2.1.0)`.
- Smooth scrolling defaults to 40 px and is configurable.
- Debug key bindings are remapped and unified, including F3 combinations, gamemode, the FPS graph, Angelica options and the EFR switcher.
- F3 text is rendered with ModernUI SDF text (pure white, formatting codes stripped) while the native layer is hidden. Angelica's F3+F6 debug-options screen keeps its stock layout and gains SDF text/tooltips, smooth scrolling and separators.

#### Fixed

- Loading-screen flicker, status-box layout, list clipping at separators, menu backdrop coverage, in-world scrim blending, snooper freezes and stream/stats alignment.
- Iris list separator flicker; separators are now drawn after the list with an alpha-0 fade.
- Cursor state no longer sticks after leaving a list or switching screens.
- Angelica's video and shader screens, together with ModernUI option pages, now receive the matching backdrop.

#### Notes

- The main menu keeps the vanilla button layout. This release applies the shared backdrop and list/loading treatments rather than rewriting every screen.
- The Angelica F3+F6 debug-options screen is intentionally stock because it is a developer screen; only text rendering, tooltips, scrolling and separators are adapted.
- The singleplayer and multiplayer screens keep vanilla buttons and row actions. This release adds the shared backdrop and icon actions, not a full screen rewrite.

Compatibility: 2.0.0 accepts remote versions `[2.0.0,2.1.0)`.

### 中文

#### 新增

- 菜单整体升级到 Minecraft 26.3 风格：共享半透明全景图、匹配的相机投影、2px 列表分隔线、约 30% 黑色遮罩、不透明选中框和干净的列表裁剪。主菜单类界面使用全景图，游戏内界面改为透明世界遮罩。
- 加载界面采用 26.3 布局：居中标记、标题、细进度条和区块网格。下界与末地使用对应传送门背景；资源重载和世界保存状态框绘制在加载界面内，并只提交一次以避免闪烁。
- 重做单人/多人列表。世界行在图标上显示进入箭头；服务器行在图标右侧显示进入箭头、左侧显示上移/下移箭头，排序结果写回 `servers.dat`。两组箭头都复用 `textures/gui/resource_packs.png`，不新增 GUI 贴图。
- 原版界面接入 26.3 鼠标指针：按钮、可点击行和滑条使用手型，文本框使用 I 型，滑条使用左右缩放。世界与服务器行只在图标上显示手型，所有界面与 ModernUI 场景共用一个指针缓存。
- 列表和文本框支持平滑滚动，可通过 `smoothscroll.listAmount` 配置（默认 40px）。
- 搜索框改为白色边框，与 26.3 列表风格一致。
- Et Futurum Requiem 兼容为可选：安装 EFR 时由 ModernUI 接管其加载界面，未安装时走原版加载路径。
- 原版聊天加入 26.3 风格指令补全：10 行、行高 12px、`0xD0000000` 背景、白色虚线滚动边、黄色选中行、灰色其他行。列表对齐当前补全词，并按输入框宽度收拢。
- Tab / Shift+Tab 循环候选并自动滚动；鼠标悬停选中、点击应用、滚轮滚动列表，悬停在列表上显示手型；不再向聊天栏打印逗号分隔的候选行。
- 聊天输入颜色对齐 26.3：命令字面量为灰色，参数按青/黄/绿/粉紫/金色循环，正在补全的词为黄色，未知玩家、物品或游戏模式显示红色。
- 候选来源合并客户端指令、缓存的服务器指令表、在线玩家、物品与方块 ID、游戏模式，以及服务器返回的参数（如 `gamerule` 规则、`time`/`weather` 取值、`scoreboard` 子命令）。安装 EFR 时 `/gamemode` 会提供 `spectator`。
- 服务器请求按发送顺序处理，由弹窗直接应用，不再让原版改写输入框。本地能补全时 Tab 不发送 C14 请求，避免拉取全部物品 ID；候选数量设有上限，保证 `/give` 流畅。

#### 变更

- 远端兼容范围改为 `[2.0.0,2.1.0)`。
- 平滑滚动默认 40px，并可在配置中调整。
- 调试快捷键重新映射并统一，包括 F3 组合键、游戏模式、FPS 图表、Angelica 选项和 EFR 切换器。
- F3 文本改用 ModernUI SDF 渲染（纯白、剥离格式码），并隐藏原版图层。Angelica 的 F3+F6 调试选项界面保留原版布局，新增 SDF 文本/提示框、平滑滚动和分隔线。

#### 修复

- 加载界面闪烁、状态框布局、分隔线处的列表裁剪、菜单背景覆盖、游戏内遮罩混合、snooper 卡死以及直播/统计界面对齐。
- Iris 列表分隔线闪烁；分隔线改为列表绘制后追加，并通过 alpha-0 淡出。
- 离开列表或切换界面后鼠标指针不再卡住。
- Angelica 的视频和着色器界面，以及 ModernUI 选项页，现在也会应用匹配的背景。

#### 说明

- 主菜单保留原版按钮布局。本次发布应用共享背景和列表/加载处理，而不是重写所有界面。
- Angelica F3+F6 调试选项界面属于开发者界面，保持原版布局，仅适配文字、提示框、滚动和分隔线。
- 单人/多人界面保留原版按钮和行操作；本次新增共享背景和图标快捷操作，不重写整个界面。

兼容性：2.0.0 接受远端版本 `[2.0.0,2.1.0)`。

## [1.2.5] - 2026-11-14

### English

#### Added

- The config screen has a Renderer section. On save, ModernUI writes `renderBackend` and `sdlGpuDriver` to Angelica. The choices are `follow`, OpenGL, Vulkan, Metal and Direct3D 12. A new backend takes effect after a restart, and ModernUI does not write a backend the current OS cannot run.
- When the screen opens, the Renderer row shows Angelica's current backend next to the saved choice.
- `renderer.api` is stored in `modernui.yaml`. `follow` leaves Angelica's settings alone; any other choice re-applies that backend on every explicit save.

#### Changed

- Metal keeps real rounded fills and rounded clipping. The rectangle fallback for SDL GPU is removed.

1.2.5 is based on 1.2.4-hotfix1 and keeps its SDL GPU font, layer, HostImage and HUD paths.

Compatibility: 1.2.5 accepts remote versions `[1.2.0,1.3.0)`.

### 中文

#### 新增

- 配置页新增「渲染后端」段。保存时 ModernUI 把 `renderBackend` 和 `sdlGpuDriver` 写入 Angelica。可选值为 `follow`、OpenGL、Vulkan、Metal 和 Direct3D 12。新后端在重启后生效，当前系统不支持的后端不会写入。
- 「渲染后端」一行会显示打开配置页时 Angelica 报告的当前后端，与保存的选择并排显示。
- `renderer.api` 写入 `modernui.yaml`。`follow` 不改 Angelica 的设置；选择其他值时，每次显式保存都会重新应用该后端。

#### 变更

- Metal 下保留真实的圆角填充和圆角裁剪，删除 SDL GPU 的直角矩形回退。

1.2.5 基于 1.2.4-hotfix1，保留其 SDL GPU 字体、图层、HostImage 与 HUD 路径。

兼容性：1.2.5 接受远端版本 `[1.2.0,1.3.0)`。

## [1.2.4-hotfix1] - 2026-11-08

### English

#### Fixed

- Private-use glyphs with a semantic equivalent are expanded to ordinary text with the existing superscript/subscript styles, so the subscript question mark (`\uE020`) and the superscript digit range render in the configured font (for example Inter Frozen) instead of the vanilla bitmap page.
- Private-use glyphs without a semantic equivalent are rendered from the vanilla Unicode font pages supplied by resource packs, so custom symbols no longer fall back to an unrelated AWT placeholder.

Compatibility: 1.2.4-hotfix1 accepts remote versions `[1.2.0,1.3.0)`.

### 中文

#### 修复

- 有语义对应的私有区字形会展开为普通字符并套用现有的上/下标样式，因此下标问号（`\uE020`）和上标数字会使用当前配置字体（例如 Inter Frozen）渲染，而不是回退到原版点阵页。
- 没有语义对应的私有区字形改由资源包提供的原版 Unicode 字体页渲染，自定义符号不再回退到 AWT 的无关占位符。

兼容性：1.2.4-hotfix1 接受远端版本 `[1.2.0,1.3.0)`。

## [1.2.4] - 2026-11-08

### English

#### Fixed

- GTNHLib custom glyphs registered by GTNH mods are expanded before text layout and measurement. GregTech chemical formulas such as steel now show `Fe₅₀C` instead of a private-use placeholder for the subscript zero.

Compatibility: 1.2.4 accepts remote versions `[1.2.0,1.3.0)`.

### 中文

#### 修复

- 在文本布局与测量前展开 GTNH 模组通过 GTNHLib 注册的自定义字形。钢等格雷科技材料的化学式现在显示为 `Fe₅₀C`，不再把下标 0 显示成私有区占位字符。

兼容性：1.2.4 接受远端版本 `[1.2.0,1.3.0)`。

## [1.2.2] - 2026-11-07

### English

#### Fixed

- The GTNH 2.9.0 font defaults are applied when the configuration loads. The font statics used before config loading stay empty, so font runtime code keeps its previous fallback behavior until a real configuration is present.
- The headless font test kit now uses an empty face assignment instead of inheriting the process-wide font configuration. A previously loaded config could make the renderer request `ITALIC` glyphs while the kit only assembles the base weight, which dropped host-italic glyphs in the Latex rendering tests. The tests now always take the synthetic-italic path.

Compatibility: 1.2.2 accepts remote versions `[1.2.0,1.3.0)`.

### 中文

#### 修复

- GTNH 2.9.0 的字体默认值改为在配置加载时应用。配置加载前使用的字体静态字段保持为空，让字体运行时在真正加载配置前维持之前的回退行为。
- 无头字体测试场地不再继承进程级字体配置，而是固定使用空字体指派。此前已加载的配置会让渲染器请求 `ITALIC` 字形，而测试场地只装配基础字重，导致 Latex 渲染测试中的宿主斜体字形被丢弃。现在测试始终走合成斜体路径。

兼容性：1.2.2 接受远端版本 `[1.2.0,1.3.0)`。

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

