package org.ton.proxy.client.app

import com.sun.jna.Platform

val libPath: String = run {
    val target = when {
        Platform.isMac() -> {
            if (Platform.isARM()) "macosArm64"
            else "macosX64"
        }

        Platform.isWindows() -> "mingwX64"
        Platform.isLinux() -> "linuxX64"
        else -> "native"
    }
    val extension = when {
        Platform.isWindows() -> ".exe"
        else -> ".kexe"
    }
    "$target/releaseExecutable/ton-proxy-client-lib$extension"
}
