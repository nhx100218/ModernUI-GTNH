
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.github.ElytraServers.elytra-conventions") version("v1.1.2")
    id("com.gtnewhorizons.gtnhconvention")
}

// Fixed semantic version for CI (git-based versioning is disabled in gradle.properties).
project.ext.set("modVersion", providers.gradleProperty("modVersion").orElse("1.2.0").get())

// 测试堆 1g：headless 软件渲染验收共享一张 GlyphRuntimeTables（约 123MiB direct-index 表），
// 512m 默认堆下与其他字体测试类同 JVM 跑会互相挤压 OOM。
tasks.withType<Test>().configureEach {
    maxHeapSize = "1024m"
    // Opt-in snapshot regeneration for local maintenance runs.
    systemProperty("qz.latex.surface.update", System.getProperty("qz.latex.surface.update", "false"))
}

// Angelica 2.2.44 的新版 F3 类不在当前 dev 依赖（2.2.10）里。相关 mixin 用字符串
// target 声明并按运行时 Angelica 版本应用，因此关闭编译期 target 校验；运行时缺失的
// target 仍会被 Mixin 以 require=0 跳过。
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-AdisableTargetValidator=true")
    // Angelica 2.2.44 才有的类不在 dev 依赖里；软 target 缺失降级为警告，
    // 运行时仍由 Mixin 按实际加载的 Angelica 版本决定是否应用（require=0）。
    options.compilerArgs.add("-AMSG_MIXIN_SOFT_TARGET_NOT_FOUND=warning")
}

// ============================================================================
// 内部开发工具（main 域 blocamlimb.nhx100218.modernui.uilib.internal.devtools）
// 含 headless 出图、scene 测试场地（playground）、磨玻璃实验室（glass）、网络自检端点
// （NetSelfCheck* / NetRuntimeSelfChecks）与开发环境完整命令（ModernUiClientCommand /
// DevToolsClientBootstrap）。定位与顶层语义见
// docs/历史报告/规划与设计/规划-headless运行时与验收设施.md；
// 硬约束（2026-09-17 用户裁定，2026-09-18 扩到整包）：不影响打包体积——类只存在于
// build/classes，不进入任何产物。发布产物内保留的 /modernui 通道命令在 client.command，
// 由 ClientProxy 按「开发环境 + 类存在」双重探测择一注册。
// ============================================================================
val internalDevToolsPackagePath = "blocamlimb/nhx100218/modernui/uilib/internal/devtools"
// Legacy custom chat bar (chat3) is no longer shipped; the mod uses vanilla chat
// with the enhancement mixins. Keep the source around for reference/tests, but
// never package it.
val legacyChatPackagePath = "blocamlimb/nhx100218/modernui/uilib/internal/chat3"

// 1) Jar 型产物统一排除（jar / shadowJar / sourcesJar / apiJar）。
//    reobfJar 是 ReobfuscatedJar（非 Jar 子类、无 exclude），吃的是 dev jar 产物，排除随输入传播——由下面的门禁实测确认。
tasks.withType<Jar>().configureEach {
    exclude("$internalDevToolsPackagePath/**")
    exclude("$legacyChatPackagePath/**")
}
// apiJar 用的是旧类型 org.gradle.jvm.tasks.Jar（不是 bundling.Jar 的子类），必须单独排除——
// 这一处漏排正是第一次门禁跑出来的真实缺陷（apiJar 里混进了 headless 包）。
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    exclude("$internalDevToolsPackagePath/**")
    exclude("$legacyChatPackagePath/**")
}

// 2) 门禁：逐个打开产物断言不含该包。排除是意图，门禁才是保证；
//    校验集合按 AbstractArchiveTask 全量枚举 + 显式依赖本仓产物任务，构建链新增打包任务不会静默漏检。
val guardedArchiveTasks = listOf("jar", "shadowJar", "sourcesJar", "apiJar", "reobfJar")
val verifyDevToolsNotPackaged by tasks.registering {
    group = "verification"
    description = "校验内部开发工具（internal.devtools 整包）未进入任何打包产物"
    dependsOn(guardedArchiveTasks)
    val guarded: List<Provider<RegularFile>> = guardedArchiveTasks.map { name ->
        tasks.named(name, AbstractArchiveTask::class.java).flatMap { task -> task.archiveFile }
    }
    val allArchives: Provider<List<File>> = provider {
        tasks.withType(AbstractArchiveTask::class.java).toList().map { task -> task.archiveFile.get().asFile }
    }
    val forbidden = "$internalDevToolsPackagePath/"
    doLast {
        val guardedFiles = guarded.map { archive -> archive.get().asFile }
        val missing = guardedFiles.count { file -> !file.isFile }
        if (missing > 0) {
            throw GradleException("verifyDevToolsNotPackaged: 有 " + missing + " 个产物未生成，门禁失效")
        }
        val extras = allArchives.get().filter { file -> file.isFile && !guardedFiles.contains(file) }
        (guardedFiles + extras).forEach { file ->
            var hit: String? = null
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entryName = entries.nextElement().name
                    if (entryName.startsWith(forbidden)) {
                        hit = entryName
                        break
                    }
                }
            }
            if (hit != null) {
                throw GradleException("内部开发工具进入了打包产物 " + file.name + "：" + hit)
            }
            logger.lifecycle("verifyDevToolsNotPackaged: " + file.name + " 不含 " + forbidden)
        }
    }
}
tasks.named("check") { dependsOn(verifyDevToolsNotPackaged) }

// ============================================================================
// 3) headless 运行期供给：LWJGL2 主 jar 与 natives 只在 compileClasspath 上（真机由 MC 客户端供给），
//    headless 直启必须自带。这里从既有 compileClasspath 提取，不新增依赖声明：
//    直启 classpath = main 输出（含 headless 类）+ 真 LWJGL2（须排在 lwjgl3ify shim 之前）+ 运行期依赖。
// ============================================================================
val headlessRuntimeDir = layout.buildDirectory.dir("headless")
val mainSourceOutput: FileCollection = sourceSets.getByName("main").output

val headlessLwjglRuntimeJars: Provider<List<File>> = configurations.named("compileClasspath").map { cfg ->
    cfg.files.filter { file ->
        val name = file.name
        name.startsWith("lwjgl-") || name.startsWith("lwjgl_util-")
    }.sortedBy { file -> file.name }
}

val headlessNativeJars: Provider<List<File>> = configurations.named("compileClasspath").map { cfg ->
    cfg.files.filter { file -> file.name.startsWith("lwjgl-platform-") }.sortedBy { file -> file.name }
}

val extractHeadlessNatives by tasks.registering(Sync::class) {
    group = "headless"
    description = "解压 LWJGL2 natives 到 build/headless/natives（直启用 -Djava.library.path）"
    into(headlessRuntimeDir.map { dir -> dir.dir("natives") })
    from(headlessNativeJars.map { jars -> jars.map { jar -> zipTree(jar) } })
    // 三个平台的 natives jar 各自带 META-INF/MANIFEST.MF，解压到同一目录必然重复。
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/**
 * 导出直启 classpath 的任务类型：用任务属性（ConfigurableFileCollection / RegularFileProperty）承载输入输出，
 * 而不是捕获 provider 到 doLast——后者在配置缓存下不可序列化（实测报 NamedDomainObjectProvider 赋值失败）。
 */
abstract class ExportHeadlessClasspath : DefaultTask() {
    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection
    @get:InputFiles
    abstract val fullClasspath: ConfigurableFileCollection
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:Input
    abstract val javaExecutable: Property<String>

    @TaskAction
    fun export() {
        val directory = outputDirectory.get().asFile
        directory.mkdirs()
        val unique = LinkedHashSet<File>(classpath.files)
        val classpathFile = File(directory, "classpath.txt")
        classpathFile.writeText(unique.joinToString(File.pathSeparator))

        // 启动器：把「一条命令出图」做成设施的一部分，而不是让 agent 自己拼 classpath。
        // Java 的 @argfile 机制让超长 classpath 不必挤进 shell 命令行；参数文件按当前平台生成。
        val argFile = File(directory, "shot-args.txt")
        argFile.writeText(buildString {
            append("-Djava.library.path=").append(File(directory, "natives").absolutePath).append('\n')
            append("-Xmx2g").append('\n')
            append("-cp").append('\n')
            append(unique.joinToString(File.pathSeparator)).append('\n')
            append("blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless.HeadlessShotMain").append('\n')
        })
        val java = javaExecutable.get()
        writeLauncher(directory, java, "shot-args.txt", "modernui-shot.bat")

        // 完整开发类路径：最小集优先，再补 compileClasspath 其余（MC/Forge 等）。
        // 用途：页面渲染一旦触及 MC 类型（IChatComponent / ChatComponentText），最小集链接不上——
        // 这不是「facility 坏了」，而是「这条页面路径本来就要 MC 类型」，故单独供给而不是把最小集撑大。
        val fullOrdered = LinkedHashSet<File>()
        fullOrdered.addAll(unique)
        fullOrdered.addAll(fullClasspath.files)
        File(directory, "classpath-full.txt").writeText(fullOrdered.joinToString(File.pathSeparator))
        File(directory, "shot-args-full.txt").writeText(buildString {
            append("-Djava.library.path=").append(File(directory, "natives").absolutePath).append('\n')
            append("-Xmx2g").append('\n')
            append("-cp").append('\n')
            append(fullOrdered.joinToString(File.pathSeparator)).append('\n')
            append("blocamlimb.nhx100218.modernui.uilib.internal.devtools.headless.HeadlessShotMain").append('\n')
        })
        writeLauncher(directory, java, "shot-args-full.txt", "modernui-shot-full.bat")
        logger.lifecycle("exportHeadlessClasspath: " + unique.size + " 项 -> " + classpathFile
                + "（启动器 modernui-shot.bat / modernui-shot.sh / modernui-shot-full.bat）")
    }

    /** 写一个平台启动器（Windows .bat 与 POSIX .sh 各一份）。 */
    private fun writeLauncher(directory: File, java: String, argFileName: String, batName: String) {
        File(directory, batName).writeText(buildString {
            append("@echo off\r\n")
            append("rem headless 出图启动器（由 exportHeadlessClasspath 生成）\r\n")
            append('"').append(java).append('"').append(" @\"%~dp0").append(argFileName).append("\" %*\r\n")
        })
        val shName = batName.replace(".bat", ".sh")
        val shell = File(directory, shName)
        shell.writeText(buildString {
            append("#!/bin/sh\n")
            append("# headless 出图启动器（由 exportHeadlessClasspath 生成）\n")
            append("exec \"").append(java).append("\" \"@\$(dirname \"\$0\")/").append(argFileName)
                    .append("\" \"\$@\"\n")
        })
        shell.setExecutable(true)
    }
}

// 4) 对拍测试供给：把「直启 classpath 文件」与「natives 目录」交给 test JVM。
//    测试侧走进程外直启（与 agent 真实使用路径一致），因此 test JVM 自身不需要 LWJGL2 依赖，
//    也避开了 lwjgl3ify shim 与真 LWJGL2 在同一条 classpath 上的先后之争。
//    注入缺失时直启类测试会 Assume 跳过：构建期在这里 fail 一次，避免整类门禁被静默跳过
//    （独立复核实测：删掉这两条 systemProperty 后构建仍 SUCCESSFUL，只有 XML 的 skipped 属性变了）。
tasks.withType<Test>().configureEach {
    dependsOn(tasks.named("exportHeadlessClasspath"))
    val headlessClasspathFile = headlessRuntimeDir.get().file("classpath.txt").asFile
    val headlessNativesDir = headlessRuntimeDir.get().dir("natives").asFile
    doFirst {
        check(headlessClasspathFile.isFile && headlessClasspathFile.length() > 0L) {
            "headless 直启 classpath 缺失或为空：${headlessClasspathFile} —— exportHeadlessClasspath 未产出，直启类测试会被整类跳过"
        }
        check(headlessNativesDir.isDirectory) {
            "headless natives 目录缺失：" + headlessNativesDir
        }
        // chat/hud 的玻璃档门禁走完整集（classpath-full.txt）直启：缺失时那条判据会 Assume 跳过，
        // 与最小集同理 —— 构建期 fail 一次，别让门禁静默变绿。
        val headlessFullClasspathFile = headlessRuntimeDir.get().file("classpath-full.txt").asFile
        check(headlessFullClasspathFile.isFile && headlessFullClasspathFile.length() > 0L) {
            "headless 完整集 classpath 缺失或为空：${headlessFullClasspathFile} —— chat/hud 的玻璃档门禁会被静默跳过"
        }
    }
    systemProperty("qz.headless.classpathFile", headlessClasspathFile.absolutePath)
    systemProperty("qz.headless.nativesDir", headlessNativesDir.absolutePath)
}

val exportHeadlessClasspath by tasks.registering(ExportHeadlessClasspath::class) {
    group = "headless"
    description = "导出 headless 直启 classpath（main 输出 + 真 LWJGL2 + 运行期依赖）"
    dependsOn(tasks.named("classes"), tasks.named("extractHeadlessNatives"))
    // 顺序即优先级：main 输出（含 headless 类）→ 真 LWJGL2（必须排在 lwjgl3ify shim 之前）→ 运行期依赖。
    // 注意必须用 sourceSets.main.output，而不是 classes 任务的 outputs——classes 是生命周期任务，outputs 为空。
    classpath.from(mainSourceOutput)
    classpath.from(headlessLwjglRuntimeJars)
    classpath.from(configurations.named("runtimeClasspath"))
    fullClasspath.from(configurations.named("compileClasspath"))
    // MC 类不在 compileClasspath 上：RFG 把重编译后的 Minecraft 放在 patchedMc 源集输出里，
    // mcLauncher 提供启动期类型。渲染触及 MC 类型的页面（chat 探针的 IChatComponent）必须带上它们。
    fullClasspath.from(sourceSets.getByName("patchedMc").output)
    fullClasspath.from(sourceSets.getByName("mcLauncher").output)
    outputDirectory.set(headlessRuntimeDir)
    javaExecutable.set(File(System.getProperty("java.home"), "bin/java").absolutePath)
}

// ============================================================================
// 5) Mixin 运行时自包含：从编译期 UniMixins dev jar 抽取 Mixin + MixinExtras，
//    以嵌套 jar 形式放进 META-INF/modernui/。运行时由 coremod 静态初始化优先
//    使用 mods 里已安装的 provider，没有时才解压这里自带的副本——UniMixins
//    不再是硬依赖。
// ============================================================================
abstract class PackageMixinRuntime : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceJars: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun packageRuntime() {
        val output = outputDirectory.get().asFile
        val embeddedDir = File(output, "META-INF/modernui")
        embeddedDir.mkdirs()

        val source = sourceJars.files.firstOrNull { file -> containsMixinRuntime(file) }
                ?: throw GradleException(
                        "PackageMixinRuntime: compileClasspath 中找不到同时包含 Mixin 与 MixinExtras 的 jar" +
                                "（期望 UniMixins dev jar）")

        ZipFile(source).use { zip ->
            writeJar(File(embeddedDir, "mixin-runtime.jar"), zip) { name -> isMixinRuntimeEntry(name) }
            writeJar(File(embeddedDir, "mixin-extras.jar"), zip) { name -> isMixinExtrasEntry(name) }
        }
        logger.lifecycle("PackageMixinRuntime: 从 " + source.name + " 生成 Mixin/MixinExtras 嵌套运行时")
    }

    private fun containsMixinRuntime(file: File): Boolean {
        if (!file.isFile || !file.name.endsWith(".jar", ignoreCase = true)) {
            return false
        }
        return try {
            ZipFile(file).use { zip ->
                zip.getEntry(MIXIN_BOOTSTRAP_ENTRY) != null && zip.getEntry(MIXIN_EXTRAS_ENTRY) != null
            }
        } catch (ignored: IOException) {
            false
        }
    }

    private fun writeJar(target: File, source: ZipFile, include: (String) -> Boolean) {
        ZipOutputStream(target.outputStream().buffered()).use { output ->
            val entries = source.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !include(entry.name)) {
                    continue
                }
                val copy = ZipEntry(entry.name)
                // 固定时间戳，避免同一输入每次生成不同字节。
                copy.time = 0L
                output.putNextEntry(copy)
                source.getInputStream(entry).use { input -> input.copyTo(output) }
                output.closeEntry()
            }
        }
    }

    companion object {
        private const val MIXIN_BOOTSTRAP_ENTRY = "org/spongepowered/asm/launch/MixinBootstrap.class"
        private const val MIXIN_EXTRAS_ENTRY = "com/llamalad7/mixinextras/MixinExtrasBootstrap.class"

        private val RUNTIME_SERVICES = setOf(
                "META-INF/services/org.spongepowered.asm.service.IGlobalPropertyService",
                "META-INF/services/org.spongepowered.asm.service.IMixinService",
                "META-INF/services/org.spongepowered.asm.service.IMixinServiceBootstrap")

        private fun isMixinRuntimeEntry(name: String): Boolean {
            if (name.startsWith("org/spongepowered/asm/") || name.startsWith("org/spongepowered/include/")) {
                return true
            }
            if (name.startsWith("META-INF/licenses/module-mixin/")) {
                return true
            }
            if (name.startsWith("META-INF/licenses/module-mixinextras/")) {
                return true
            }
            return isMixinExtrasEntry(name) || RUNTIME_SERVICES.contains(name)
        }

        private fun isMixinExtrasEntry(name: String): Boolean {
            // ap/ 只服务编译期注解处理，运行期不需要。
            if (name.startsWith("META-INF/licenses/module-mixinextras/")) {
                return true
            }
            return name.startsWith("com/llamalad7/mixinextras/") &&
                    !name.startsWith("com/llamalad7/mixinextras/ap/")
        }
    }
}

val mixinRuntimeOutput = layout.buildDirectory.dir("generated/mixinRuntime")

val generateMixinRuntime by tasks.registering(PackageMixinRuntime::class) {
    group = "build"
    description = "从 UniMixins dev jar 生成随 mod 打包的 Mixin/MixinExtras 嵌套运行时"
    sourceJars.from(configurations.named("compileClasspath").map { configuration ->
        configuration.files.filter { file -> file.isFile && file.name.endsWith(".jar", ignoreCase = true) }
    })
    outputDirectory.set(mixinRuntimeOutput)
}

// 只挂到实际发布链路（jar -> shadowJar -> reobfJar），不污染 sourcesJar。
listOf("jar", "shadowJar").forEach { archiveTaskName ->
    tasks.named(archiveTaskName, Jar::class.java) {
        from(mixinRuntimeOutput)
        dependsOn(generateMixinRuntime)
        manifest {
            // 保持纯 coremod：声明 TweakClass 的 jar 会被 FML 当成 tweaker
            // 处理，从而跳过 FMLCorePlugin 的实例化。运行时由 EarlyMixins
            // 在 coremod 发现阶段自行准备。
            attributes.remove("TweakClass")
        }
    }
}
