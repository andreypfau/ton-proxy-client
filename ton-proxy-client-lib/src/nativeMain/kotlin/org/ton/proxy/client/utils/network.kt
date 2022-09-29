package org.ton.proxy.client.utils

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.InetAddress
import com.github.andreypfau.kotlinio.address.InetSocketAddress
import com.github.andreypfau.kotlinio.packet.ip.IpPacket
import com.github.andreypfau.kotlinio.packet.transport.TransportPacket
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.popen

internal val IpPacket.srcSocketAddress
    get(): InetSocketAddress {
        val payload = payload
        return header.srcAddress to if (payload is TransportPacket) payload.header.srcPort else 0u
    }

internal val IpPacket.dstSocketAddress
    get(): InetSocketAddress {
        val payload = payload
        return header.dstAddress to if (payload is TransportPacket) payload.header.dstPort else 0u
    }


val InetAddress.isPublic: Boolean
    get() = when (this) {
        is Inet4Address -> isPublic
        else -> true
    }
val Inet4Address.isPublic: Boolean
    get() {
        if (isMulticastAddress) return false
        if (isAnyLocalAddress) return false
        if (isLinkLocalAddress) return false
        if (isSiteLocalAddress) return false
        return true
    }

internal fun systemStr(command: String): String = memScoped {
    val file = popen(command, "r")
    val bufLength = 1024
    val buf = allocArray<ByteVar>(bufLength)
    val stringBuilder = StringBuilder()
    while (true) {
        val str = fgets(buf, bufLength, file)?.toKString() ?: break
        stringBuilder.append(str)
    }
    stringBuilder.toString()
}
