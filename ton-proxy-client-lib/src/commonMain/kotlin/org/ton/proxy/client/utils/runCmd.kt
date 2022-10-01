package org.ton.proxy.client.utils

import com.github.andreypfau.kotlinio.address.Inet4Address
import platform.posix.system
import kotlin.experimental.xor

internal fun runCmd(cmd: String) {
    println(cmd)
    system(cmd)
}

internal fun Inet4Address.changeBit(bit: Int): Inet4Address {
    val bytes = toByteArray()
    bytes[bytes.size - 1] = bytes[bytes.size - 1] xor (1 shl bit).toByte()
    return Inet4Address(bytes)
}
