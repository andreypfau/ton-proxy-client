package os

import java.io.File

actual class File(
    actual val name: String
) {
    private val jvmFile: File = File(name)

    actual val descriptor: FileDescriptor
        get() = TODO()

    actual fun close() {

    }
}