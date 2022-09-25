package org.ton.proxy.client.handler

import HttpHandler
import com.github.andreypfau.kotlinio.packet.ip.IpPacket
import com.github.andreypfau.kotlinio.packet.ip.IpProtocol
import com.github.andreypfau.kotlinio.packet.ipv4.IpV4Packet
import com.github.andreypfau.kotlinio.packet.udp.UdpPacket
import org.ton.proxy.client.utils.isPublic

class IpHandler(
    val networkHandler: NetworkHandler
) {
    val dnsHandler = DnsHandler(networkHandler)
    val httpHandler = HttpHandler(networkHandler)

    fun handleLocal(ipPacket: IpPacket) {
        if (ipPacket !is IpV4Packet) return
        val protocol = ipPacket.header.protocol
        if (protocol != IpProtocol.TCP && protocol != IpProtocol.UDP && protocol != IpProtocol.ICMPV4) return
        if (ipPacket.header.srcAddress != networkHandler.virtualAddress) {
            val payload = ipPacket.payload
            if (payload !is UdpPacket) return
            if (payload.header.dstPort != 53.toUShort()) {
                return
            }
        }
        if (!ipPacket.header.dstAddress.isPublic) return
        var result = ipPacket
        result = dnsHandler.handleLocalPacket(result) ?: return
        result = httpHandler.handleLocal(result) ?: return
        networkHandler.send(result)
    }

    fun handleRemote(ipPacket: IpPacket) {
        if (ipPacket !is IpV4Packet) return
        val protocol = ipPacket.header.protocol
        if (protocol != IpProtocol.TCP && protocol != IpProtocol.UDP && protocol != IpProtocol.ICMPV4) return
        var result = ipPacket
        result = dnsHandler.handleRemote(result) ?: return
        result = httpHandler.handleRemote(result) ?: return
        networkHandler.receive(result)
    }
}
