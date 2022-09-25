package org.ton.proxy.client.device

import com.github.andreypfau.kotlinio.utils.PosixException
import kotlinx.cinterop.cstr
import os.File
import os.FileDescriptor
import platform.posix.errno
import tun.tun_open

actual class VirtualDevice(
    actual val file: File,
    actual val name: String,
) {
    actual val mtu: Int = DEFAULT_MTU

    actual fun readPacket(buf: ByteArray, offset: Int): Int {
        return file.read(buf, offset)
    }

    actual fun writePacket(buf: ByteArray, offset: Int): Int {
        return file.write(buf, offset)
    }

    actual fun flush() {
    }

    actual fun close() {
        file.close()
    }

    actual companion object {
        fun createDevice(name: String): VirtualDevice {
            val fd = tun_open(name.cstr)
            if (fd == -1) throw PosixException(errno)
            val file = File("", FileDescriptor(fd))
            return VirtualDevice(file, name)
        }
    }
}
