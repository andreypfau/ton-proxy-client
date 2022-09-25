import kotlinx.coroutines.runBlocking
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.utils.bestInterface
import org.ton.proxy.client.utils.getInterfaces

fun main() = runBlocking {
    val ifAddr = getInterfaces().bestInterface()
    getInterfaces().forEach {
        println(it)
    }
    println("best: $ifAddr")
    val virtualDevice = VirtualDevice.createDevice("utun")
    getInterfaces().forEach {
        println(it)
    }
}
