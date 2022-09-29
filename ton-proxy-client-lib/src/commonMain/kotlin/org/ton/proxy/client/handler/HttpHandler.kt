import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.InetAddress
import com.github.andreypfau.kotlinio.address.InetSocketAddress
import com.github.andreypfau.kotlinio.address.port
import com.github.andreypfau.kotlinio.packet.ip.IpPacket
import com.github.andreypfau.kotlinio.packet.tcp.TcpPacket
import org.ton.proxy.client.handler.NetworkHandler

class HttpHandler(
    val networkHandler: NetworkHandler
) {
    fun handleLocal(ipPacket: IpPacket?): IpPacket? {
        val tcpPacket = ipPacket?.payload as? TcpPacket ?: return ipPacket
        val proxyAddress = getProxyByAddress(ipPacket.header.dstAddress) ?: return ipPacket
        if (tcpPacket.header.dstPort != HTTP_PORT) return ipPacket
        val tcpBuilder = tcpPacket.builder().apply {
            dstPort = proxyAddress.port
        }
        return ipPacket.builder().apply {
            payloadBuilder = tcpBuilder
        }.build()
    }

    fun handleRemote(ipPacket: IpPacket?): IpPacket? {
        val tcpPacket = ipPacket?.payload as? TcpPacket ?: return ipPacket
        val proxyAddress = getProxyByAddress(ipPacket.header.srcAddress) ?: return ipPacket
        if (tcpPacket.header.srcPort != proxyAddress.port) return ipPacket
        val tcpBuilder = tcpPacket.builder().apply {
            srcPort = HTTP_PORT
        }
        return ipPacket.builder().apply {
            payloadBuilder = tcpBuilder
        }.build()
    }

    companion object {
        val PROXY_LIST: List<InetSocketAddress> = listOf(
            Inet4Address("5.2.76.237") to 8080u,
            Inet4Address("77.91.73.24") to 8080u,
            Inet4Address("167.235.34.220") to 8080u
        )
        const val HTTP_PORT: UShort = 80u

        fun getProxyByAddress(address: InetAddress?): InetSocketAddress? {
            if (address == null) return null
            return PROXY_LIST.find { it.first == address }
        }

        // TODO: check availability
        fun getRandomProxy(): InetSocketAddress {
            return PROXY_LIST.random()
        }
    }
}
