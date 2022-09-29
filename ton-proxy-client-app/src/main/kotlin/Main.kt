@file:Suppress("OPT_IN_USAGE")

import com.sun.jna.Platform
import org.ton.proxy.client.app.AppVersion
import org.ton.proxy.client.app.application
import org.ton.proxy.client.app.libPath
import java.io.File

fun main() {
    val lockFile = lockFile() ?: return
    val libFile = extractLib(lockFile.parentFile)
    val cmds = arrayOf(
        "osascript", "-e", "do shell script \"chmod +x $libFile && $libFile $lockFile\" with administrator privileges"
    )
    println(cmds.joinToString(" "))
    Runtime.getRuntime().exec(cmds)
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
    val libFileExt = if (Platform.isWindows()) ".exe" else ".kexe"
    val libFile = File(dir, "ton-proxy-client-${AppVersion}$libFileExt")
    if (libFile.exists()) return libFile
    else {
        dir.listFiles { _, name ->
            name.endsWith(libFileExt)
        }?.forEach {
            it.delete()
        }
    }
    val loader = Thread.currentThread().contextClassLoader
    val resource = loader.getResource(libPath)!!
    resource.openStream().use { input ->
        libFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return libFile
}
