package org.ton.proxy.utils

import io.ktor.utils.io.core.*
import java.io.IOException
import java.net.*
import java.nio.channels.SocketChannel

fun Inet4Address(host: String) =
    Inet4Address.getByName(host) as Inet4Address

fun Inet6Address(host: String) =
    Inet6Address.getByName(host) as Inet6Address

fun findBestInterface(): InetAddress? {
    val interfaces = NetworkInterface.networkInterfaces()
    for (networkInterface in interfaces) {
        if (networkInterface.isLoopback) continue
        if (!networkInterface.isUp) continue
        if (!networkInterface.inetAddresses.hasMoreElements()) continue
        for (inetAddress in networkInterface.inetAddresses) {
            if (inetAddress is Inet6Address) continue
            if (!inetAddress.isReachable(3000)) continue
            try {
                SocketChannel.open().use { socket ->
                    socket.socket().soTimeout = 3000
                    socket.bind(InetSocketAddress(inetAddress, 0))
                    socket.connect(InetSocketAddress("1.1.1.1", 80))

                    return inetAddress
                }
            } catch (e: IOException) {
                continue
            }
        }
    }
    return null
}

