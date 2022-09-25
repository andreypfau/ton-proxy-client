package os

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.O_RDWR
import platform.posix.open

actual class File(
    actual val name: String,
    actual val descriptor: FileDescriptor = FileDescriptor(open(name, O_RDWR))
) {
    actual fun close() {
        descriptor.close()
    }

    fun read(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Int =
        buf.usePinned {
            platform.posix.read(descriptor.fd, it.addressOf(offset), length.convert())
        }.convert()

    fun write(buf: ByteArray, offset: Int = 0, length: Int = buf.size - offset): Int =
        buf.usePinned {
            platform.posix.write(descriptor.fd, it.addressOf(offset), length.convert())
        }.convert()

    companion object {
        fun create(name: String): File {
            val fd = open(name, O_RDWR)
            return File(name, FileDescriptor(fd))
        }
    }
}
