package org.ton.proxy.client.utils

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.InetAddress
import com.github.andreypfau.kotlinio.socket.Socket
import com.github.andreypfau.kotlinio.socket.receiveFlow
import kotlinx.cinterop.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.linux.getifaddrs
import platform.linux.ifaddrs
import platform.posix.*

internal fun getInterfaces(): List<InetInterface> = memScoped {
    val addresses = allocArrayOfPointersTo(alloc<ifaddrs>())
    getifaddrs(addresses)
    addresses.pointed.pointed?.iterator()?.asSequence()?.mapNotNull { ifaddrs ->
        val sockaddr = ifaddrs.ifa_addr?.pointed ?: return@mapNotNull null
        if (sockaddr.sa_family.toInt() != AF_INET) return@mapNotNull null
        val address = Inet4Address(sockaddr.sa_data.readBytes(2 + Inet4Address.SIZE_BYTES), 2)
        val name = ifaddrs.ifa_name?.toKString() ?: ""
        InetInterface(address, name)
    }?.toList() ?: emptyList()
}

data class InetInterface(
    val address: InetAddress,
    val name: String
)

private val DNS_QUERY = sequenceOf(
    0xAA, 0xAA, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x07, 0x65, 0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x03, 0x63, 0x6f, 0x6d,
    0x00, 0x00, 0x01, 0x00, 0x01
).map { it.toByte() }.toList().toByteArray()

internal suspend fun List<InetInterface>.bestInterface() = coroutineScope {
    val asyncInetInterface = CompletableDeferred<InetInterface>()
    val job = launch {
        map { inetInterface ->
            val socket = Socket.udp()
            launch {
                setsockopt(socket.fd, SOL_SOCKET, SO_BINDTODEVICE, inetInterface.name.cstr, IF_NAMESIZE)
                socket.connect(Inet4Address(byteArrayOf(8, 8, 8, 8)), 53u)
                socket.write(DNS_QUERY, 0)
                socket.receiveFlow().first()
                asyncInetInterface.complete(inetInterface)
            }.invokeOnCompletion {
                socket.close()
            }
        }
    }
    val address = withTimeoutOrNull(3000) {
        asyncInetInterface.await()
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
