//import org.jetbrains.compose.compose
//import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
//    id("org.jetbrains.compose")
}

group = "org.ton"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val allTarget = false
    val isMingw = hostOs.startsWith("Windows")
    val isLinux = hostOs.startsWith("Linux")
    val isMacos = hostOs.startsWith("Mac OS")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:0.18.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3-native-mt")
                implementation("com.github.andreypfau:kotlin-io:1.0-SNAPSHOT")
                implementation("com.github.andreypfau:pcap-kotlin:1.0-SNAPSHOT")
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val linuxMain by creating {
            dependsOn(nativeMain)
        }
        linuxX64 {
            compilations.getByName("main") {
                cinterops {
                    val tun by creating {
                        defFile(project.file("src/nativeInterop/cinterop/tun.def"))
                        packageName("tun")
                        headers(project.file("src/nativeInterop/cinterop/tun.h"))
                        compilerOpts("-I/path")
                        includeDirs.allHeaders("path")
                    }
                }
                binaries {
                    executable()
                }
            }
        }
        val linuxX64Main by getting {
            dependsOn(linuxMain)
        }

//        mingwX64()
//        val mingwX64Main by getting
//        val mingwMain by creating {
//            dependsOn(commonMain)
//            mingwX64Main.dependsOn(this)
//        }

        if (isMacos) {
            macosX64()
            macosArm64 {
                binaries {
                    executable()
                }
            }

            val macosArm64Main by getting
            val macosX64Main by getting

            val macosMain by creating {
                dependsOn(commonMain)
                macosArm64Main.dependsOn(this)
                macosX64Main.dependsOn(this)
            }
        }
    }
}

//compose.desktop {
//    application {
//        mainClass = "MainKt"
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "TON Proxy Client"
//            packageVersion = "1.0.0"
//        }
//    }
//}
