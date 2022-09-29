import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("io.ktor:ktor-client-core-jvm:2.1.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.1")
    implementation("io.ktor:ktor-client-cio-jvm:2.1.1")
    implementation("net.java.dev.jna:jna:5.12.1")
}

val libProject = rootProject.project("ton-proxy-client-lib")

tasks.jar {
    from("../ton-proxy-client-lib/build/bin/") {
        include("**/*.dylib")
        include("**/*.so")
        include("**/*.dll")
    }
    manifest.attributes.set("Main-Class", "MainKt")
}

tasks.shadowJar {
    minimize {
        exclude {
            it.moduleGroup.startsWith("org.jetbrains.compose") ||
                it.moduleGroup.startsWith("net.java.dev")
        }
    }
    from("../ton-proxy-client-lib/build/bin/") {
        include("**/*.kexe")
        include("**/*.exe")
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
        dependsOn(tasks.shadowJar.get())
        mainJar.set(tasks.shadowJar.get().archiveFile)
        fromFiles(ff)

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TON Proxy Client"
            packageVersion = project.version.toString()
            modules = arrayListOf(
                "java.base", "java.desktop", "java.logging"
            )
            macOS {
                iconFile.set(project.file("src/main/resources/ton_symbol.icns"))

            }
        }
    }
}
