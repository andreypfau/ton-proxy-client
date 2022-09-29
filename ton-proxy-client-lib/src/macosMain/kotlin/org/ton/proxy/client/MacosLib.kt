package org.ton.proxy.client

import com.github.andreypfau.kotlinio.address.Inet4Address
import kotlinx.coroutines.runBlocking
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.handler.NetworkHandler

@CName("ton_proxy_client_start")
actual fun tonProxyClientStart() = runBlocking {
    val virtualDevice = VirtualDevice.createDevice("utun")
    val networkHandler = NetworkHandler(
        Inet4Address("10.8.0.1"),
        virtualDevice,
        coroutineContext
    )
    networkHandler.printNetworkAddresses()
    networkHandler.configureRouting()
}
