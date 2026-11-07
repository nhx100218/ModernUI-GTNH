# CHANGELOG

本项目采用 [Keep a Changelog](https://keepachangelog.com/) 风格记录变更，版本号遵循
`主.次.修订[-标签]` 格式：主版本号变更代表破坏性 API 调整，次版本号代表能力扩展，
修订号代表行为修复或文档调整。

## [Unreleased]

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

