package os

expect class File {
    val name: String
    val descriptor: FileDescriptor

    fun close()
}