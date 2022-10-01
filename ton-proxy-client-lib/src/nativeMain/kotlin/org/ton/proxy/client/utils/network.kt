package org.ton.proxy.client.utils

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.InetAddress
import com.github.andreypfau.kotlinio.address.InetSocketAddress
import com.github.andreypfau.kotlinio.packet.ip.IpPacket
import com.github.andreypfau.kotlinio.packet.transport.TransportPacket

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
