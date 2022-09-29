package org.ton.proxy.client.device

import os.File

actual class VirtualDevice {
    actual val file: File
        get() = TODO("Not yet implemented")
    actual val mtu: Int
        get() = TODO("Not yet implemented")
    actual val name: String
        get() = TODO("Not yet implemented")

    actual fun readPacket(buf: ByteArray, offset: Int): Int {
        TODO("Not yet implemented")
    }

    actual fun writePacket(buf: ByteArray, offset: Int): Int {
        TODO("Not yet implemented")
    }

    actual fun flush() {
    }

    actual fun close() {
    }

    actual companion object
}