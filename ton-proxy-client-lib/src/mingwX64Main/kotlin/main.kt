import com.github.andreypfau.kotlinio.address.Inet4Address
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ton.proxy.client.tonProxyClientStart
import platform.posix.F_OK
import platform.posix.access
import platform.posix.exit

fun main(args: Array<String>) {
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
