package org.ton.proxy.client.app

val libPath: String = run {
    val os = when {
        NativePlatform.isWindows() -> "mingw"
        NativePlatform.isMac() -> "macos"
        NativePlatform.isLinux() -> "linux"
        else -> NativePlatform.os
    }
    val arch = when {
        NativePlatform.isX64() -> "X64"
        NativePlatform.isArm() -> "Arm64"
        else -> NativePlatform.arch
    }
    val extension = when {
        NativePlatform.isWindows() -> ".exe"
        else -> ".kexe"
    }
    "$os$arch/ton-proxy-client-lib$extension"
}
