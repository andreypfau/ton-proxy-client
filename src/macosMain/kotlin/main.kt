import com.github.andreypfau.kotlinio.address.Inet4Address
import kotlinx.coroutines.runBlocking
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.handler.NetworkHandler

fun main() = runBlocking {
    val virtualDevice = VirtualDevice.createDevice("utun")
    val networkHandler = NetworkHandler(
        Inet4Address("10.8.0.1"),
        virtualDevice,
        coroutineContext
    )
    networkHandler.printNetworkAddresses()
    networkHandler.configureRouting()
}
