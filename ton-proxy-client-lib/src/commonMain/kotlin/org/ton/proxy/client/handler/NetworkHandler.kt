@file:Suppress("OPT_IN_USAGE")

package org.ton.proxy.client.handler

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.MacAddress
import com.github.andreypfau.kotlinio.packet.Packet
import com.github.andreypfau.kotlinio.packet.ethernet.EtherType
import com.github.andreypfau.kotlinio.packet.ethernet.EthernetBuilder
import com.github.andreypfau.kotlinio.packet.ethernet.header.EthernetHeader
import com.github.andreypfau.kotlinio.packet.ip.IpPacket
import com.github.andreypfau.kotlinio.packet.ip.IpProtocol
import com.github.andreypfau.kotlinio.packet.ipv4.IpV4Packet
import com.github.andreypfau.kotlinio.packet.transport.TransportBuilder
import com.github.andreypfau.kotlinio.utils.encodeHex
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.ton.proxy.client.device.VirtualDevice
import pcap.Pcap
import pcap.PcapAddressFamily
import pcap.PcapPromiscousMode
import kotlin.coroutines.CoroutineContext

class NetworkHandler(
    val virtualDevice: VirtualDevice,
    coroutineContext: CoroutineContext
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineName("Network Handler")

    val realDevice = requireNotNull(
        Pcap.findAllDevs().find { dev ->
            dev.addresses.any { addr -> addr.family == PcapAddressFamily.INET4 } &&
                dev.addresses.any { addr -> addr.family == PcapAddressFamily.MAC }
        }
    ) {
        "Can't get PCAP device"
    }
    val realAddress = requireNotNull(realDevice.addresses.find { it.family == PcapAddressFamily.INET4 }?.let {
        Inet4Address(it.toByteArray())
    }) {
        "Can't get real device IPv4 address"
    }
    val realMac = requireNotNull(realDevice.addresses.find { it.family == PcapAddressFamily.MAC }?.let {
        MacAddress(it.toByteArray())
    }) {
        "Can't get real device MAC address"
    }
    val ipHandler = IpHandler(this)
    val pcapHandle = realDevice.openLive(Short.MAX_VALUE.toInt(), PcapPromiscousMode.PROMISCUOUS, 10)
    val receiveJob = launch(newSingleThreadContext("Receive")) {
        println("Start receive job...")
        val buf = ByteArray(0x1000)
        var time = Clock.System.now()
        var bytes = 0L
        while (isActive) {
            val now = Clock.System.now()
            val length = pcapHandle.nextPacket(buf, 0, buf.size)
            if (length > 0) {
                bytes += length
                val packet = buf.copyOf(length)
                try {
                    val ethernetHeader = EthernetHeader.newInstance(packet, 0)
                    if (ethernetHeader.type != EtherType.IPv4) continue
                    if (ethernetHeader.dstAddress != realMac) continue
                    val ipPacket = IpV4Packet(packet, ethernetHeader.length, length)
                    launch(this@NetworkHandler.coroutineContext) {
                        ipHandler.handleRemote(ipPacket)
                    }
                } catch (e: Exception) {
                    throw RuntimeException("Can't process receive packet ($length bytes):\n${packet.encodeHex()}")
                }
            }
            if ((now - time).inWholeMilliseconds > 1000) {
                println("RX: $bytes bytes/sec")
                bytes = 0
                time = now
            }
            delay(1)
        }
    }
    val sendJob = launch(newSingleThreadContext("Send")) {
        println("Start send job...")
        val buf = ByteArray(0x1000)
        var time = Clock.System.now()
        var bytes = 0L
        while (isActive) {
            val now = Clock.System.now()
            val length = virtualDevice.readPacket(buf, 0)
            if (length > 0) {
                bytes += length
                val rawData = buf.copyOf(length)
                launch(this@NetworkHandler.coroutineContext) {
                    try {
                        val ipPacket = IpPacket.newInstance(rawData)
                        ipHandler.handleLocal(ipPacket)
                    } catch (e: Exception) {
                        throw RuntimeException("Can't process send packet ($length bytes):\n${rawData.encodeHex()}")
                    }
                }
            }
            if ((now - time).inWholeMilliseconds > 1000) {
                println("TX: $bytes bytes/sec")
                bytes = 0
                time = now
            }
            delay(1)
        }
    }

    fun send(packet: Packet) {
        val ipPacket = packet[IpV4Packet::class] ?: return
        val protocol = ipPacket.header.protocol
        if (protocol != IpProtocol.TCP && protocol != IpProtocol.UDP && protocol != IpProtocol.ICMPV4) return
        val ethernetPacket = EthernetBuilder().apply {
            srcAddress = realMac
            dstAddress = virtualDevice.gatewayMac
            type = EtherType.IPv4
            payloadBuilder = ipPacket.builder().apply {
                srcAddress = realAddress
                dontFragmentFlag = true
                payloadBuilder = payloadBuilder?.also {
                    if (it is TransportBuilder) {
                        it.srcAddress = realAddress
                    }
                }
            }
            padding = ByteArray(0)
            paddingAtBuild = false
        }.build()
        val rawEthernetPacket = ethernetPacket.toByteArray()
        pcapHandle.sendPacket(rawEthernetPacket, 0, rawEthernetPacket.size)
    }

    fun receive(packet: Packet) {
        var ipPacket = packet[IpV4Packet::class] ?: return
        ipPacket = ipPacket.builder().apply {
            dstAddress = virtualDevice.address
            payloadBuilder = payloadBuilder?.also {
                if (it is TransportBuilder) {
                    it.dstAddress = virtualDevice.address
                }
            }
        }.build()
        val rawPacket = ipPacket.toByteArray()
        virtualDevice.writePacket(rawPacket, 0)
    }

    fun printNetworkAddresses() {
        println("Real IPv4    : $realAddress")
        println("Virtual IPv4 : ${virtualDevice.address}")
        println("Real MAC     : $realMac")
        println("Gateway MAC  : ${virtualDevice.gatewayMac}")
    }
}
