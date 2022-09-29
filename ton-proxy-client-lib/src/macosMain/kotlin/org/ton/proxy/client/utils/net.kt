package org.ton.proxy.client.utils

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.InetAddress
import com.github.andreypfau.kotlinio.packet.ip.IpVersion
import com.github.andreypfau.kotlinio.socket.Socket
import com.github.andreypfau.kotlinio.socket.receiveFlow
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_INET

internal fun getInterfaces(): List<InetAddress> = memScoped {
    val addresses = allocArrayOfPointersTo(alloc<ifaddrs>())
    getifaddrs(addresses)
    addresses.pointed.pointed?.iterator()?.asSequence()?.mapNotNull { ifaddrs ->
        val sockaddr = ifaddrs.ifa_addr?.pointed ?: return@mapNotNull null
        if (sockaddr.sa_family.toInt() != AF_INET) return@mapNotNull null
        Inet4Address(sockaddr.sa_data.readBytes(2 + Inet4Address.SIZE_BYTES), 2)
    }?.toList() ?: emptyList()
}

private val DNS_QUERY = sequenceOf(
    0xAA, 0xAA, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x07, 0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x03, 0x63, 0x6f, 0x6d,
    0x00, 0x00, 0x01, 0x00, 0x01
).map { it.toByte() }.toList().toByteArray()

internal suspend fun List<InetAddress>.bestInterface() = coroutineScope {
    val asyncAddress = CompletableDeferred<InetAddress>()
    val job = launch {
        map { address ->
            val socket = Socket.udp(IpVersion.IPv4)
            launch {
                socket.bind(address, 0u)
                socket.connect(Inet4Address(byteArrayOf(8, 8, 8, 8)), 53u)
                socket.write(DNS_QUERY, 0)
                socket.receiveFlow().first()
                asyncAddress.complete(address)
            }.invokeOnCompletion {
                socket.close()
            }
        }
    }
    val address = withTimeoutOrNull(3000) {
        asyncAddress.await()
    }
    job.cancel()
    address
}

internal fun ifaddrs?.iterator() = iterator {
    var next = this@iterator
    while (true) {
        if (next != null) {
            yield(next)
            next = next.ifa_next?.pointed
        } else break
    }
}
