package org.ton.proxy.client.handler

import HttpHandler
import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.Inet6Address
import com.github.andreypfau.kotlinio.packet.dns.*
import com.github.andreypfau.kotlinio.packet.dns.rdata.DnsRDataA
import com.github.andreypfau.kotlinio.packet.dns.rdata.DnsRDataAAAA
import com.github.andreypfau.kotlinio.packet.ip.IpPacket
import com.github.andreypfau.kotlinio.packet.ip.IpProtocol
import com.github.andreypfau.kotlinio.packet.ipv4.IpV4Packet
import com.github.andreypfau.kotlinio.packet.ipv6.IpV6Packet
import com.github.andreypfau.kotlinio.packet.udp.UdpBuilder
import com.github.andreypfau.kotlinio.packet.udp.UdpPacket

class DnsHandler(
    val networkHandler: NetworkHandler
) {
    fun handleLocalPacket(ipPacket: IpPacket?): IpPacket? {
        val udpPacket = ipPacket?.payload as? UdpPacket ?: return ipPacket
        if (udpPacket.header.dstPort != DNS_PORT) return ipPacket
        val payload = udpPacket.payload?.rawData ?: return ipPacket
        val dnsPacket = try {
            DnsPacket(payload)
        } catch (e: Exception) {
            return ipPacket
        }
        val adnlQuestions = dnsPacket.header.questions.filter {
            val name = it.qName.name
            name.endsWith(".ton") || name.endsWith(".adnl")
        }
        if (adnlQuestions.isEmpty()) return ipPacket
        val adnlAnswers = adnlQuestions.map(::resolveAdnlQuestion)
        val dnsPacketBuilder = DnsPacket.Builder().apply {
            id = dnsPacket.header.id
            isResponse = true
            opCode = DnsOpCode.QUERY
            rCode = DnsRCode.NO_ERROR
            questions += adnlQuestions
            answers += adnlAnswers
        }
        val udpPacketResponse = UdpBuilder().apply {
            srcAddress = ipPacket.header.dstAddress
            dstAddress = ipPacket.header.srcAddress
            srcPort = udpPacket.header.dstPort
            dstPort = udpPacket.header.srcPort
            payloadBuilder = dnsPacketBuilder
        }
        val responseIpPacket = when (ipPacket) {
            is IpV4Packet -> {
                IpV4Packet {
                    srcAddress = ipPacket.header.dstAddress
                    dstAddress = ipPacket.header.srcAddress
                    protocol = IpProtocol.UDP
                    payloadBuilder = udpPacketResponse
                }
            }

            is IpV6Packet -> {
                IpV6Packet {
                    srcAddress = ipPacket.header.dstAddress
                    dstAddress = ipPacket.header.srcAddress
                    protocol = IpProtocol.UDP
                    payloadBuilder = udpPacketResponse
                }
            }

            else -> return null
        }
        networkHandler.receive(responseIpPacket)
        return null
    }

    fun handleRemote(ipPacket: IpPacket?): IpPacket? {
        return ipPacket
    }

    fun resolveAdnlQuestion(adnlQuestion: DnsQuestion): DnsResourceRecord {
        val (address, _) = HttpHandler.getRandomProxy()
        return DnsResourceRecord.Builder().apply {
            name = adnlQuestion.qName
            dataClass = DnsClass.IN
            ttl = 10u
            when (address) {
                is Inet4Address -> {
                    dataType = DnsResourceRecordType.A
                    rData = DnsRDataA(address)
                }

                is Inet6Address -> {
                    dataType = DnsResourceRecordType.AAAA
                    rData = DnsRDataAAAA(address)
                }

                else -> throw IllegalArgumentException("Unsupported address: $address")
            }
        }.build()
    }

    companion object {
        const val DNS_PORT: UShort = 53u
    }
}
