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
import kotlinx.datetime.Clock
import org.ton.proxy.client.device.VirtualDevice
import org.ton.proxy.client.utils.systemStr
import pcap.Pcap
import pcap.PcapAddressFamily
import pcap.PcapPromiscousMode
import platform.posix.system
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.xor

actual class NetworkHandler(
    actual val virtualAddress: Inet4Address,
    actual val virtualDevice: VirtualDevice,
    coroutineContext: CoroutineContext
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineName("Network Handler")

    val gatewayMac = gatewayMacAddress()
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
        val buf = ByteArray(Short.MAX_VALUE.toInt())
        var time = Clock.System.now()
        var bytes = 0L
        while (isActive) {
            val now = Clock.System.now()
            val length = pcapHandle.nextPacket(buf, 0, buf.size)
            if (length > 0) {
                bytes += length
                val packet = buf.copyOf(length)
                val ethernetHeader = EthernetHeader.newInstance(packet, 0)
                if (ethernetHeader.type != EtherType.IPv4) continue
                if (ethernetHeader.dstAddress != realMac) continue
                val ipPacket = IpV4Packet(packet, ethernetHeader.length, length)
                launch(this@NetworkHandler.coroutineContext) {
                    ipHandler.handleRemote(ipPacket)
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
        val buf = ByteArray(Short.MAX_VALUE.toInt())
        var time = Clock.System.now()
        var bytes = 0L
        while (isActive) {
            val now = Clock.System.now()
            val length = virtualDevice.readPacket(buf, 0)
            if (length > 0) {
                bytes += length
                // TODO: set IFF_NO_PI for offset 0
                val rawData = buf.copyOfRange(4, length)
                launch(this@NetworkHandler.coroutineContext) {
                    val ipPacket = IpPacket.newInstance(rawData)
                    ipHandler.handleLocal(ipPacket)
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

    fun configureRouting() {
        // for IPv4 macOS seems to require a device address and manual setup of the route
        runCmd("sudo /sbin/ifconfig ${virtualDevice.name} add $virtualAddress ${virtualAddress.changeBit(2)}")
        runCmd("sudo /sbin/ifconfig ${virtualDevice.name} up")
        runCmd("sudo /sbin/route -n add 0.0.0.0/1 $virtualAddress")
        runCmd("sudo /sbin/route -n add 128.0.0.0/1 $virtualAddress")
        runCmd("sudo networksetup -setdnsservers Wi-Fi 8.8.8.8")
    }

    fun runCmd(cmd: String) {
        print(cmd)
        val result = system(cmd)
        print(" ... Result: $result\n")
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
        val rawData = ByteArray(ipPacket.length + 4)
        rawData[3] = 0x02
        ipPacket.toByteArray(rawData, 4)
        virtualDevice.writePacket(rawData, 0)
    }

    private fun gatewayMacAddress(): MacAddress {
        val output = systemStr("arp \"\$(netstat -rn | grep 'default' | cut -d' ' -f13)\" | cut -d' ' -f4")
        return MacAddress(output.replace("\n", "").split(':').map { it.toUByte(16) }.toUByteArray().toByteArray())
    }

    fun printNetworkAddresses() {
        println(realDevice.addresses)
        println("Real IPv4    : $realAddress")
        println("Virtual IPv4 : $virtualAddress")
        println("Real MAC     : $realMac")
        println("Gateway MAC  : $gatewayMac")
    }

    private fun Inet4Address.changeBit(bit: Int): Inet4Address {
        val bytes = toByteArray()
        bytes[bytes.size - 1] = bytes[bytes.size - 1] xor (1 shl bit).toByte()
        return Inet4Address(bytes)
    }
}
