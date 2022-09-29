package org.ton.proxy.client.app

object NativePlatform {
    val arch = System.getProperty("os.arch")
    val os = System.getProperty("os.name")

    fun isArm() = arch.startsWith("arm") || arch.startsWith("aarch")
    fun isX64() = arch == "x86_64" || arch == "amd64"
    fun isWindows() = os.startsWith("Windows")
    fun isMac() = os.startsWith("Mac") || os.startsWith("Darwin")
    fun isLinux() = os.startsWith("Linux")
}
