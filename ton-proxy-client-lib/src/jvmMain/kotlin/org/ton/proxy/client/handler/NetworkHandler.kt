package org.ton.proxy.client.handler

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.packet.Packet
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.ton.proxy.client.device.VirtualDevice
import kotlin.coroutines.CoroutineContext

actual class NetworkHandler(
    coroutineContext: CoroutineContext
) : CoroutineScope {
    actual val virtualAddress: Inet4Address
        get() = TODO("Not yet implemented")
    actual val virtualDevice: VirtualDevice
        get() = TODO("Not yet implemented")
    override val coroutineContext = coroutineContext + CoroutineName("Network Handler")

    actual fun send(packet: Packet) {
    }

    actual fun receive(packet: Packet) {
    }
}
