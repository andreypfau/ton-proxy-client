import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("net.java.dev.jna:jna:5.12.1")
}

object NativePlatform {
    val arch = System.getProperty("os.arch")
    val os = System.getProperty("os.name")

    fun isArm() = arch.startsWith("arm") || arch.startsWith("aarch")
    fun isX64() = arch == "x86_64" || arch == "amd64"
    fun isWindows() = os.startsWith("Windows")
    fun isMac() = os.startsWith("Mac") || os.startsWith("Darwin")
    fun isLinux() = os.startsWith("Linux")
    fun getTarget(): String {
        val os = when {
            isWindows() -> "mingw"
            isMac() -> "macos"
            isLinux() -> "linux"
            else -> NativePlatform.os
        }
        val arch = when {
            isX64() -> "X64"
            isArm() -> "Arm64"
            else -> NativePlatform.arch
        }
        return "$os$arch"
    }
}

val libProject = rootProject.project(":ton-proxy-client-lib")

tasks.shadowJar {
    minimize {
        exclude {
            it.moduleGroup.startsWith("org.jetbrains.compose") ||
                it.moduleGroup.startsWith("net.java.dev")
        }
    }
    val versionFile = project.file("build/tmp/version")
    versionFile.writeText(project.version.toString())
    from(versionFile)
    val path = "../ton-proxy-client-lib/build/bin/${NativePlatform.getTarget()}/releaseExecutable/"
    from(path) {
        val extension = if (NativePlatform.isWindows()) ".exe" else ".kexe"
        include("ton-proxy-client-lib$extension")
        into(NativePlatform.getTarget())
    }
    manifest.attributes.set("Main-Class", "MainKt")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        disableDefaultConfiguration()
        val ff = project.fileTree("build/libs/") {
            include("*.jar")
        }
        mainJar.set(tasks.shadowJar.get().archiveFile)
        fromFiles(ff)

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TON Proxy Client"
            modules = arrayListOf(
                "java.base", "java.desktop"
            )
            copyright = "© 2022 TON Foundation"
            macOS {
                iconFile.set(project.file("../assets/ton_symbol.icns"))
                bundleID = "org.ton.proxy.client"
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSUIElement</key>
                        <true/>
                    """.trimIndent()
                }
            }
        }
    }
}
