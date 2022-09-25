package os

actual class FileDescriptor(
    val fd: Int
) {
    actual fun close() {
        platform.posix.close(fd)
    }
}