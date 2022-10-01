package org.ton.proxy.client.device

import com.github.andreypfau.kotlinio.utils.PosixException
import kotlinx.cinterop.cstr
import os.File
import os.FileDescriptor
import platform.posix.errno
import tun.tun_open
import java.net.Inet4Address

actual class VirtualDevice(
    val fd: Int,
    actual val address: Inet4Address,
    actial val dnsAddress: Inet4Address,
    actual val name: String,
) {
    actual val gatewayMac = gatewayMacAddress()

    actual fun readPacket(buf: ByteArray, offset: Int): Int {
        return file.read(buf, offset)
    }

    actual fun writePacket(buf: ByteArray, offset: Int): Int {
        return file.write(buf, offset)
    }

    actual fun close() {
        close(fd)
    }

    actual fun configureRouting() {
        val deviceGateway = address.changeBit(2)
        system("ip tuntap add mode tun dev $name")
        system("ip address add $address/24 dev $name")
        system("ip link set dev $name")
        system("ip link set dev $name up")
        system("ip route add default via $deviceGateway dev ${name}")
        system("resolvectl dns $name $dnsAddress")
    }

    private fun gatewayMacAddress(): MacAddress {
        val output =
            systemStr("/sbin/ip neigh|grep \"\$(/sbin/ip -4 route list 0/0|cut -d' ' -f3) \"|cut -d' ' -f5|tr '[a-f]' '[A-F]'")
        return MacAddress(output.replace("\n", "").split(':').map { it.toUByte(16) }.toUByteArray().toByteArray())
    }

    actual companion object {
        fun createDevice(name: String): VirtualDevice {
            val fd = tun_open(name.cstr)
            if (fd == -1) throw PosixException(errno)
            return VirtualDevice(fd, name)
        }
    }
}
