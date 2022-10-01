package org.ton.proxy.client.device

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.MacAddress

expect class VirtualDevice {
    val name: String
    val address: Inet4Address
    val dnsAddress: Inet4Address
    val gatewayMac: MacAddress
    fun readPacket(packet: ByteArray, offset: Int = 0): Int
    fun writePacket(packet: ByteArray, offset: Int = 0): Int
    fun close()
    fun configureRouting()

    companion object
}
