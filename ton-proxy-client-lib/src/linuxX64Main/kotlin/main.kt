import com.github.andreypfau.kotlinio.address.Inet4Address
import kotlinx.coroutines.runBlocking
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.handler.NetworkHandler

fun main() = runBlocking {
    val lockFile = args.firstOrNull()
    if (lockFile != null) {
        GlobalScope.launch {
            println("Check lock file: $lockFile")
            while (isActive) {
                if (access(lockFile, F_OK) == 0) {
                    delay(1000)
                } else {
                    exit(0)
                }
            }
        }
    }
    tonProxyClientStart(
        address = Inet4Address("10.8.0.1"),
        dnsAddress = Inet4Address("1.1.1.1")
    )
}
