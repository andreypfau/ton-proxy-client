package os

actual class File(
    fd: Int,
    actual val name: String
) {
    actual val descriptor = FileDescriptor(fd)

    actual fun close() {
        descriptor.close()
    }
}