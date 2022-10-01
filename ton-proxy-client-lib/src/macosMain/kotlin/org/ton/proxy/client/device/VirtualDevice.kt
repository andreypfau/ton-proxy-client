package org.ton.proxy.client.device

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.MacAddress
import com.github.andreypfau.kotlinio.packet.ip.IpVersion
import kotlinx.cinterop.*
import org.ton.proxy.client.utils.*
import platform.osx.CTLIOCGINFO
import platform.osx.ctl_info
import platform.osx.sockaddr_ctl
import platform.posix.*

actual class VirtualDevice(
    val fd: Int,
    actual val address: Inet4Address,
    actual val dnsAddress: Inet4Address
) {
    actual val gatewayMac: MacAddress = gatewayMac()
    actual val name: String
        get() = getSockoptString(
            fd,
            2, // SYSPROTO_CONTROL
            2, // UTUN_OPT_IFNAME
        )
    private var routeSocket: Int = -1
    private val bufLen = 0x1000
    private val buf = nativeHeap.allocArray<ByteVar>(bufLen)

    override fun toString(): String = "VirtualDevice($routeSocket, $name)"

    actual fun readPacket(packet: ByteArray, offset: Int): Int {
        val length = read(fd, buf, bufLen.convert()).toInt().check { it >= 0 || it == EAGAIN }
        if (length > 0) {
            packet.usePinned {
                memcpy(it.addressOf(offset), buf + 4, (packet.size - offset).convert())
            }
            return length - 4
        }
        return 0
    }

    actual fun writePacket(packet: ByteArray, offset: Int): Int {
        memScoped {
            val rawDataLength = packet.size + 4
            val rawData = allocArray<ByteVar>(rawDataLength)
            rawData[3] = when (IpVersion[packet[offset]]) {
                IpVersion.IPv4 -> AF_INET.toByte()
                IpVersion.IPv6 -> AF_INET6.toByte()
            }
            packet.usePinned {
                memcpy(rawData + 4, it.addressOf(offset), packet.size.convert())
            }
            write(fd, rawData, rawDataLength.convert())
        }
        return packet.size
    }

    actual fun close() {
        close(fd)
        nativeHeap.free(buf)
    }

    actual fun configureRouting() {
        val deviceGateway = address.changeBit(2)
        runCmd("sudo /sbin/ifconfig $name add $address $deviceGateway")
        runCmd("sudo /sbin/ifconfig $name up")
        runCmd("sudo /sbin/route -n add 0.0.0.0/1 $address")
        runCmd("sudo /sbin/route -n add 128.0.0.0/1 $address")
        runCmd("sudo networksetup -setdnsservers Wi-Fi $dnsAddress")
    }

    private fun gatewayMac(): MacAddress {
        try {
            val output = systemStr("arp \"\$(netstat -rn | grep 'default' | cut -d' ' -f13)\" | cut -d' ' -f4")
            return MacAddress(output.replace("\n", "").split(':').map { it.toUByte(16) }.toUByteArray().toByteArray())
        } catch (e: Exception) {
            throw RuntimeException("Can't get gateway MAC address. Is there a connection to a third-party VPN?", e)
        }
    }

    actual companion object {
        private const val UTUN_CONTROL_NAME = "com.apple.net.utun_control"

        fun createDevice(name: String, address: Inet4Address, dnsAddress: Inet4Address): VirtualDevice = memScoped {
            var ifIndex = -1
            if (name != "utun") {
                ifIndex = name.removePrefix("utun").toInt()
            }
            val fd = socketCloseExec(AF_SYSTEM, SOCK_DGRAM, 2)
            if (fd <= 0) {
                error("socket error: $fd")
            }
            val info = alloc<ctl_info> {
                strncpy(ctl_name, UTUN_CONTROL_NAME, UTUN_CONTROL_NAME.length.toULong())
            }
            try {
                ioctl(fd, CTLIOCGINFO, info.rawPtr.toLong()).check()
            } catch (e: Exception) {
                error("ioctl getctlinfo: $e")
            }
            val sc = alloc<sockaddr_ctl> {
                sc_family = AF_SYSTEM.toUByte()
                sc_id = info.ctl_id
                sc_len = sockaddr_ctl.size.toUByte() // SizeofSockaddrCtl
                ss_sysaddr = 0x2.toUShort() // AF_SYS_CONTROL
                sc_unit = (ifIndex + 1).toUInt()
            }
            println("${info.ctl_name.toKStringFromUtf8()} - ${info.ctl_id}")
            try {
                connect(fd, sc.ptr.reinterpret(), sc.ptr.pointed.sc_len.toUInt()).check()
            } catch (e: Exception) {
                close(fd)
                error("connection error: $e")
            }
            val device = VirtualDevice(fd, address, dnsAddress)
            device
        }
    }
}

internal fun socketCloseExec(family: Int, sotype: Int, proto: Int): Int {
    val fd = socket(family, sotype, proto)
    println("create fd: $fd")
    if (fd > 0) {
        closeOnExec(fd)
    }
    return fd
}
