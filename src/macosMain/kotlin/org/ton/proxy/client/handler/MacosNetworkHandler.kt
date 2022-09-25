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
import kotlinx.coroutines.*
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.utils.gatewayMacAddress
import pcap.Pcap
import pcap.PcapAddressFamily
import pcap.PcapPromiscousMode
import platform.posix.system
import kotlin.coroutines.CoroutineContext

actual class NetworkHandler(
    actual val virtualAddress: Inet4Address,
    actual val virtualDevice: VirtualDevice,
    coroutineContext: CoroutineContext
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineName("Network Handler")

    val gatewayMac = gatewayMacAddress()
    val realDevice = requireNotNull(
        Pcap.lookupDev()
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
        val buf = ByteArray(Short.MAX_VALUE.toInt())
        while (isActive) {
            val length = pcapHandle.nextPacket(buf, 0, buf.size)
            if (length > 0) {
                val packet = buf.copyOf(length)
                val ethernetHeader = EthernetHeader.newInstance(packet, 0)
                if (ethernetHeader.type != EtherType.IPv4) continue
                if (ethernetHeader.dstAddress != realMac) continue
                val ipPacket = IpV4Packet(packet, 0, ethernetHeader.length)
                launch(this@NetworkHandler.coroutineContext) {
                    ipHandler.handleRemote(ipPacket)
                }
            }
            delay(1)
        }
    }
    val sendJob = launch(newSingleThreadContext("Send")) {
        val buf = ByteArray(Short.MAX_VALUE.toInt())
        while (isActive) {
            val length = virtualDevice.readPacket(buf)
            if (length > 0) {
                val rawData = buf.copyOf(length)
                val ipPacket = IpPacket.newInstance(rawData)
                launch(this@NetworkHandler.coroutineContext) {
                    ipHandler.handleLocal(ipPacket)
                }
            }
            delay(1)
        }
    }

    fun configureRouting() {
        system("ip tuntap add mode tun dev ${virtualDevice.name}")
        system("ip address add 10.8.0.1/24 dev ${virtualDevice.name}")
        system("ip link set dev ${virtualDevice.name} mtu ${virtualDevice.mtu}")
        system("ip link set dev ${virtualDevice.name} up")
        system("ip route add default via 10.8.0.2 dev ${virtualDevice.name}")
        system("resolvectl dns ${virtualDevice.name} 8.8.8.8")
    }

    actual fun send(packet: Packet) {
        val ipPacket = packet[IpV4Packet::class] ?: return
        val protocol = ipPacket.header.protocol
        if (protocol != IpProtocol.TCP && protocol != IpProtocol.UDP && protocol != IpProtocol.ICMPV4) return
        val ethernetPacket = EthernetBuilder().apply {
            srcAddress = realMac
            dstAddress = gatewayMac
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

    actual fun receive(packet: Packet) {
        var ipPacket = packet[IpV4Packet::class] ?: return
        ipPacket = ipPacket.builder().apply {
            dstAddress = virtualAddress
            payloadBuilder = payloadBuilder?.also {
                if (it is TransportBuilder) {
                    it.dstAddress = virtualAddress
                }
            }
        }.build()
        virtualDevice.writePacket(ipPacket.rawData)
    }

    fun printNetworkAddresses() {
        println("Real IPv4    : $realAddress")
        println("Virtual IPv4 : $virtualAddress")
        println("Real MAC     : $realMac")
        println("Gateway MAC  : $gatewayMac")
    }
}
