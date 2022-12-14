package org.ton.proxy.client

import com.github.andreypfau.kotlinio.address.Inet4Address
import kotlinx.coroutines.runBlocking
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.handler.NetworkHandler

@CName("ton_proxy_client_start")
actual fun tonProxyClientStart(address: Inet4Address, dnsAddress: Inet4Address) = runBlocking {
    val virtualDevice = VirtualDevice.createDevice("utun", address, dnsAddress)
    val networkHandler = NetworkHandler(
        virtualDevice,
        coroutineContext
    )
    networkHandler.printNetworkAddresses()
    virtualDevice.configureRouting()
}
