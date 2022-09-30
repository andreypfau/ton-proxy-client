import org.ton.proxy.client.app.AppVersion
import org.ton.proxy.client.app.NativePlatform
import org.ton.proxy.client.app.application
import org.ton.proxy.client.app.libPath
import java.io.File

fun main() {
    val lockFile = lockFile() ?: return
    val libFile = extractLib(lockFile.parentFile)
    runLib(libFile, lockFile)
    application()
}

fun lockFile(): File? {
    val file = File(System.getProperty("user.home"), ".ton-proxy-client/.lock")
    file.parentFile.mkdirs()
    if (file.exists()) return null
    file.createNewFile()
    file.deleteOnExit()
    return file
}

fun extractLib(dir: File): File {
    val libFileExt = if (NativePlatform.isWindows()) ".exe" else ".kexe"
    val libFile = File(dir, "ton-proxy-client-${AppVersion}$libFileExt")
    val loader = Thread.currentThread().contextClassLoader
    val resource = loader.getResource(libPath)
        ?: error("Can't find lib for OS: '${NativePlatform.os}', Arch: '${NativePlatform.arch}', Path: '$libPath'")
    resource.openStream().use { input ->
        libFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    libFile.deleteOnExit()
    return libFile
}

fun runLib(libFile: File, lockFile: File) {
    val cmd = when {
        NativePlatform.isMac() -> arrayOf(
            "osascript",
            "-e",
            "do shell script \"chmod +x $libFile && $libFile $lockFile\" with administrator privileges"
        )

        else -> error("Unsupported platform: $NativePlatform")
    }
    println("Run cmd: ${cmd.joinToString(" ")}")
    Runtime.getRuntime().exec(cmd)
}
