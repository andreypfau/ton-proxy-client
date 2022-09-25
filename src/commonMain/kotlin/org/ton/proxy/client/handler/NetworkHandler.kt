package org.ton.proxy.client.handler

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.packet.Packet
import kotlinx.coroutines.CoroutineScope
import org.ton.proxy.client.device.VirtualDevice

expect class NetworkHandler : CoroutineScope {
    val virtualAddress: Inet4Address
    val virtualDevice: VirtualDevice

    fun send(packet: Packet)

    fun receive(packet: Packet)
}
