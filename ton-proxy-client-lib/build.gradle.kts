plugins {
    kotlin("multiplatform") version "1.7.20"
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
                implementation("com.github.andreypfau:kotlinio:1.0-SNAPSHOT")
                implementation("com.github.andreypfau:pcap-kotlin:1.0-SNAPSHOT")
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }

        if (isLinux || allTarget) {
            val linuxMain by creating {
                dependsOn(nativeMain)
            }
            linuxX64 {
                compilations.getByName("main") {
                    sourceSets.getByName("main") {
                        dependsOn(linuxMain)
                        dependsOn(nativeMain)
                    }
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
        }

        if (isMingw || allTarget) {
            mingwX64 {
                compilations.getByName("main") {
                    defaultSourceSet {
                        dependsOn(commonMain)
                    }
                    cinterops {
                        val wintun by creating {
                            defFile(project.file("src/nativeInterop/cinterop/wintun.def"))
                            packageName("wintun")
                            headers(project.file("src/nativeInterop/cinterop/wintun.h"))
                        }
                    }
                }
            }
        }

        if (isMacos) {
            val macosMain by creating {
                dependsOn(nativeMain)
            }
            listOf(macosX64(), macosArm64()).forEach { target ->
                target.apply {
                    compilations.forEach {
                        it.defaultSourceSet {
                            dependsOn(macosMain)
                        }
                    }
                    binaries {
                        executable()
                    }
                }
            }
        }
    }
}
