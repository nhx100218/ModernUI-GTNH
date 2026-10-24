package blocamlimb.nhx100218.modernui.uilib.ui.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import blocamlimb.nhx100218.modernui.uilib.util.UiNumbers;

/**
 * 负责把同帧已捕获 tile 组装为新的快照 atlas。
 *
 * <p><b>状态契约</b>：本类的公开入口会改写 FBO 绑定/附件、viewport、program、blend 与颜色等状态，
 * 而它们都<strong>不在</strong>帧围栏的快照集合内（围栏只恢复固定管线状态与绑定类对象）。
 * 因此所有入口必须在 {@code UiMainLayerSnapshotService} 的
 * {@code glPushAttrib(GL_ALL_ATTRIB_BITS)} 信封内调用（现网调用链均满足，见 GL 自净审查 N18）。
 * 唯一自带保存/恢复的是矩阵助手 {@code drawAtlasTextureQuad}：它以显式 push/pop 自守，因为本方法
 * 运行在 attrib 信封内、对帧围栏没有依赖（帧围栏自 P6 起也按内容恢复 PROJECTION/MODELVIEW，
 * 但那是帧边界上的第二道，不是这里的依据）。</p>
 */
final class SnapshotTileAtlasAssembler {

    private SnapshotTileAtlasAssembler() {
    }

    /**
     * 为当前请求生成每个 tile 的 atlas 组装来源。
     *
     * @param snapshots         快照池
     * @param frameId           当前帧编号
     * @param readFramebufferId read framebuffer
     * @param sampleRegion      请求采样区域
     * @param contentRevision   当前内容版本
     * @param downsampleFactor  请求降采样倍率
     * @param blurRadius        请求模糊半径
     * @return tile 组装条目列表
     */
    static List<TileAssemblyEntry> resolveTileAssemblyEntries(List<FrameSnapshot> snapshots, int frameId,
                                                              int readFramebufferId, SampleRegion sampleRegion, int contentRevision, int downsampleFactor,
                                                              int blurRadius) {
        List<TileAssemblyEntry> entries = new ArrayList<TileAssemblyEntry>();
        TileRegion requestedTileRegion = UiMainLayerSnapshotGeometry.resolveTileRegion(sampleRegion);
        for (int tileY = requestedTileRegion.getTileTop(); tileY < requestedTileRegion.getTileBottom(); tileY++) {
            for (int tileX = requestedTileRegion.getTileLeft(); tileX < requestedTileRegion.getTileRight(); tileX++) {
                SampleRegion tileSampleRegion = UiMainLayerSnapshotGeometry.resolveTileSampleRegion(sampleRegion,
                        tileX, tileY, tileX + 1, tileY + 1);
                if (tileSampleRegion == null) {
                    continue;
                }
                FrameSnapshot sourceSnapshot = findTileSourceSnapshot(snapshots, frameId, readFramebufferId,
                        tileSampleRegion, contentRevision, downsampleFactor, blurRadius);
                entries.add(new TileAssemblyEntry(tileSampleRegion, sourceSnapshot));
            }
        }
        return entries;
    }

    /**
     * 统计可以从既有 snapshot 复用的 tile 数。
     *
     * @param entries tile 组装条目
     * @return 可复用 tile 数
     */
    static int countReusableTileEntries(List<TileAssemblyEntry> entries) {
        int reusableTileCount = 0;
        if (entries == null) {
            return 0;
        }
        for (TileAssemblyEntry entry : entries) {
            if (entry != null && entry.sourceSnapshot != null) {
                reusableTileCount++;
            }
        }
        return reusableTileCount;
    }

    /**
     * 查找能完整覆盖指定 tile 像素区域的既有 snapshot。
     *
     * @param snapshots         快照池
     * @param frameId           当前帧编号
     * @param readFramebufferId read framebuffer
     * @param tileSampleRegion  tile 像素区域
     * @param contentRevision   当前内容版本
     * @param downsampleFactor  请求降采样倍率
     * @param blurRadius        请求模糊半径
     * @return 最小覆盖 snapshot；没有时返回 null
     */
    static FrameSnapshot findTileSourceSnapshot(List<FrameSnapshot> snapshots, int frameId, int readFramebufferId,
                                                SampleRegion tileSampleRegion, int contentRevision, int downsampleFactor, int blurRadius) {
        FrameSnapshot sourceSnapshot = null;
        for (FrameSnapshot snapshot : snapshots) {
            if (snapshot.capturedFrameId == frameId && snapshot.readFramebufferId == readFramebufferId
                    && snapshot.contentRevision == contentRevision
                    // atlas 读取 sourceTextureId（未滤镜截图），不依赖 blur 或降采样倍率。
                    // 按滤镜参数拒绝它会在冻结版本的批次中重新采入已绘制的兄弟玻璃。
                    && snapshot.sourceTextureId != 0 && UiMainLayerSnapshotGeometry.containsSampleRegion(
                    toSampleRegion(snapshot), tileSampleRegion)
                    && isBetterContainingSnapshot(sourceSnapshot, snapshot)) {
                sourceSnapshot = snapshot;
            }
        }
        return sourceSnapshot;
    }

    /**
     * 把缺失 tile 从当前 read framebuffer 写入 atlas 原始纹理。
     *
     * <p>状态契约：会改写纹理绑定与 read/draw framebuffer 绑定；必须在调用方的
     * {@code glPushAttrib(GL_ALL_ATTRIB_BITS)} 信封内调用（见类 javadoc）。</p>
     *
     * @param snapshot                  目标 snapshot
     * @param screenHeight              屏幕高度
     * @param readFramebufferId         read framebuffer
     * @param previousReadFramebufferId 原 read framebuffer
     * @param atlasRegion               atlas 采样区域
     * @param entries                   tile 组装条目
     * @return 从 framebuffer 复制的 tile 数
     */
    static int copyMissingTileEntriesFromFramebuffer(FrameSnapshot snapshot, int screenHeight, int readFramebufferId,
                                                     int previousReadFramebufferId, SampleRegion atlasRegion, List<TileAssemblyEntry> entries) {
        int copiedTileCount = 0;
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, snapshot.sourceTextureId);
        if (previousReadFramebufferId != readFramebufferId) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebufferId);
        }
        for (TileAssemblyEntry entry : entries) {
            if (entry == null || entry.sourceSnapshot != null) {
                continue;
            }
            SampleRegion copiedRegion = entry.sampleRegion;
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                    copiedRegion.getLeft() - atlasRegion.getLeft(), UiMainLayerSnapshotGeometry
                            .resolveTextureCopyTargetY(atlasRegion, copiedRegion),
                    copiedRegion.getLeft(), UiMainLayerSnapshotGeometry.resolveCopySourceY(screenHeight,
                            copiedRegion),
                    copiedRegion.getWidth(), copiedRegion.getHeight());
            copiedTileCount++;
        }
        return copiedTileCount;
    }

    /**
     * 把可复用 tile 从既有 snapshot 源纹理绘制到新的 atlas 原始纹理。
     *
     * <p>状态契约：会改写 draw framebuffer 绑定与附件、viewport、program、blend 与颜色；
     * 必须在调用方的 {@code glPushAttrib(GL_ALL_ATTRIB_BITS)} 信封内调用（见类 javadoc）。</p>
     *
     * @param snapshot    目标 snapshot
     * @param atlasRegion atlas 采样区域
     * @param entries     tile 组装条目
     * @param errorSink   atlas FBO 失败回调
     * @return 从既有 snapshot 复用的 tile 数
     */
    static int renderCoveredTileEntriesToAtlas(FrameSnapshot snapshot, SampleRegion atlasRegion,
                                               List<TileAssemblyEntry> entries, Consumer<String> errorSink) {
        int reusableTileCount = 0;
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        ensureTextureAssemblyFramebuffer(snapshot);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, snapshot.filterFramebufferId);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                snapshot.sourceTextureId, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            if (errorSink != null) {
                errorSink.accept("tile-atlas-fbo-incomplete:" + status);
            }
            throw new IllegalStateException("tile atlas fbo incomplete: " + status);
        }

        GL20.glUseProgram(0);
        GL11.glViewport(0, 0, atlasRegion.getWidth(), atlasRegion.getHeight());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        for (TileAssemblyEntry entry : entries) {
            if (entry == null || entry.sourceSnapshot == null) {
                continue;
            }
            renderSnapshotTileToAtlas(atlasRegion, entry.sampleRegion, entry.sourceSnapshot);
            reusableTileCount++;
        }
        return reusableTileCount;
    }

    /**
     * 确保 snapshot 有可用于 tile atlas 组装的 FBO。
     *
     * @param snapshot 目标 snapshot
     */
    static void ensureTextureAssemblyFramebuffer(FrameSnapshot snapshot) {
        if (snapshot.filterFramebufferId == 0) {
            snapshot.filterFramebufferId = GL30.glGenFramebuffers();
            if (snapshot.filterFramebufferId == 0) {
                throw new IllegalStateException("tile atlas framebuffer allocation failed");
            }
        }
    }

    /**
     * 从既有 snapshot 源纹理中绘制一个 tile 子区域到目标 atlas。
     *
     * @param atlasRegion      atlas 采样区域
     * @param tileSampleRegion tile 像素区域
     * @param sourceSnapshot   来源 snapshot
     */
    static void renderSnapshotTileToAtlas(SampleRegion atlasRegion, SampleRegion tileSampleRegion,
                                          FrameSnapshot sourceSnapshot) {
        SampleRegion sourceRegion = toSampleRegion(sourceSnapshot);
        float leftU = UiNumbers.clamp01(((float) tileSampleRegion.getLeft() - (float) sourceRegion.getLeft())
                / (float) Math.max(1, sourceRegion.getWidth()));
        float rightU = UiNumbers.clamp01(((float) tileSampleRegion.getRight() - (float) sourceRegion.getLeft())
                / (float) Math.max(1, sourceRegion.getWidth()));
        float topV = UiNumbers.clamp01(1.0F - ((float) tileSampleRegion.getTop() - (float) sourceRegion.getTop())
                / (float) Math.max(1, sourceRegion.getHeight()));
        float bottomV = UiNumbers.clamp01(1.0F - ((float) tileSampleRegion.getBottom() - (float) sourceRegion.getTop())
                / (float) Math.max(1, sourceRegion.getHeight()));
        int targetLeft = tileSampleRegion.getLeft() - atlasRegion.getLeft();
        int targetTop = tileSampleRegion.getTop() - atlasRegion.getTop();
        int targetRight = tileSampleRegion.getRight() - atlasRegion.getLeft();
        int targetBottom = tileSampleRegion.getBottom() - atlasRegion.getTop();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sourceSnapshot.sourceTextureId);
        drawAtlasTextureQuad(atlasRegion.getWidth(), atlasRegion.getHeight(), targetLeft, targetTop, targetRight,
                targetBottom, leftU, rightU, topV, bottomV);
    }

    /**
     * 在当前 draw framebuffer 上按 top-left 坐标绘制纹理子矩形。
     *
     * <p>状态契约：本方法自带投影/模型视图压栈与 matrix mode 还原（<strong>矩阵栈内容</strong>不受 attrib 帧
     * 管辖，必须自守；帧围栏自 P6 起也按内容恢复这两条栈，但那是帧边界上的第二道，不能替代本方法在
     * attrib 信封内的自守。matrix mode 两者都能恢复，这里只是尽早还原）。
     * 纹理绑定、blend、光照等 FFP 状态不在本方法职责内，由调用点所在事务回收——现网调用链
     * 全部位于 {@code UiMainLayerSnapshotService} 的 {@code glPushAttrib(GL_ALL_ATTRIB_BITS)} 信封内
     * （GL 自净审查 N18/N23）。</p>
     *
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @param left         目标左侧
     * @param top          目标顶部
     * @param right        目标右侧
     * @param bottom       目标底部
     * @param leftU        源纹理左侧 U
     * @param rightU       源纹理右侧 U
     * @param topV         源纹理顶部 V
     * @param bottomV      源纹理底部 V
     */
    static void drawAtlasTextureQuad(int targetWidth, int targetHeight, int left, int top, int right,
                                     int bottom, float leftU, float rightU, float topV, float bottomV) {
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        boolean projectionPushed = false;
        boolean modelViewPushed = false;
        Throwable failure = null;
        try {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            projectionPushed = true;
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, (double) targetWidth, (double) targetHeight, 0.0D, -1.0D, 1.0D);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            modelViewPushed = true;
            GL11.glLoadIdentity();

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(leftU, bottomV);
            GL11.glVertex2f((float) left, (float) bottom);
            GL11.glTexCoord2f(rightU, bottomV);
            GL11.glVertex2f((float) right, (float) bottom);
            GL11.glTexCoord2f(rightU, topV);
            GL11.glVertex2f((float) right, (float) top);
            GL11.glTexCoord2f(leftU, topV);
            GL11.glVertex2f((float) left, (float) top);
            GL11.glEnd();
        } catch (RuntimeException drawFailure) {
            failure = drawFailure;
        } catch (Error drawFailure) {
            failure = drawFailure;
        }
        // 逐步累积（不中断后续）：单步失败若直接传播，后面的弹出与 matrix mode 还原全部被跳过，
        // 本帧后续绘制会继承本方法的投影/模型视图（GL 自净审查 N9 的同类失败模式）。
        if (modelViewPushed) {
            try {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
            } catch (RuntimeException popFailure) {
                failure = appendFailure(failure, popFailure);
            } catch (Error popFailure) {
                failure = appendFailure(failure, popFailure);
            }
        }
        if (projectionPushed) {
            try {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
            } catch (RuntimeException popFailure) {
                failure = appendFailure(failure, popFailure);
            } catch (Error popFailure) {
                failure = appendFailure(failure, popFailure);
            }
        }
        try {
            GL11.glMatrixMode(previousMatrixMode);
        } catch (RuntimeException modeFailure) {
            failure = appendFailure(failure, modeFailure);
        } catch (Error modeFailure) {
            failure = appendFailure(failure, modeFailure);
        }
        if (failure != null) {
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw (Error) failure;
        }
    }

    /** 合并失败：首个失败为主异常，后续挂 suppressed；致命 {@link Error} 升级为主异常。 */
    private static Throwable appendFailure(Throwable primary, Throwable additional) {
        if (primary == null) {
            return additional;
        }
        if (additional == null || primary == additional) {
            return primary;
        }
        if (additional instanceof Error && !(additional instanceof LinkageError)
                && !(primary instanceof Error && !(primary instanceof LinkageError))) {
            additional.addSuppressed(primary);
            return additional;
        }
        primary.addSuppressed(additional);
        return primary;
    }

    private static boolean isBetterContainingSnapshot(FrameSnapshot currentSnapshot, FrameSnapshot candidateSnapshot) {
        if (currentSnapshot == null) {
            return true;
        }
        long currentArea = (long) currentSnapshot.width * (long) currentSnapshot.height;
        long candidateArea = (long) candidateSnapshot.width * (long) candidateSnapshot.height;
        return candidateArea < currentArea;
    }

    private static SampleRegion toSampleRegion(FrameSnapshot snapshot) {
        return new SampleRegion(snapshot.sampleLeft, snapshot.sampleTop, snapshot.sampleLeft + snapshot.width,
                snapshot.sampleTop + snapshot.height);
    }
}
