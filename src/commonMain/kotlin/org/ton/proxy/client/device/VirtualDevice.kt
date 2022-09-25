package org.ton.proxy.client.device

import os.File

expect class VirtualDevice {
    val file: File
    val mtu: Int
    val name: String
    fun readPacket(buf: ByteArray, offset: Int = 0): Int
    fun writePacket(buf: ByteArray, offset: Int = 0): Int
    fun flush()
    fun close()

    companion object
}

val VirtualDevice.Companion.DEFAULT_MTU: Int
    get() = 1500