package org.ton.proxy.client.device

import com.github.andreypfau.kotlinio.address.Inet4Address
import com.github.andreypfau.kotlinio.address.MacAddress
import kotlinx.cinterop.*
import org.ton.proxy.client.utils.changeBit
import org.ton.proxy.client.utils.runCmd
import org.ton.proxy.client.wintun.*
import platform.posix.GUID
import platform.posix.memcpy
import platform.windows.DWORDVar

actual class VirtualDevice(
    actual val name: String,
    actual val address: Inet4Address,
    actual val dnsAddress: Inet4Address
) {
    actual val gatewayMac = gatewayMac()
    val adapter = memScoped {
        // md5("org.ton.proxy.client") == 0ff43370-7eaf-5059-c9ed5cf2c08174a7
        wintunCreateAdapter.invoke(name.wcstr.ptr, null, alloc<GUID> {
            Data1 = 0x0ff43370u
            Data2 = 0x7eafu
            Data3 = 0x5059u
            Data4[0] = 0xc9u
            Data4[1] = 0xedu
            Data4[2] = 0x5cu
            Data4[3] = 0xf2u
            Data4[4] = 0xc0u
            Data4[5] = 0x81u
            Data4[6] = 0x74u
            Data4[7] = 0xa7u
        }.ptr)
    }
    val session = wintunStartSession.invoke(adapter, 0x400000u)
    actual fun readPacket(packet: ByteArray, offset: Int): Int {
        memScoped {
            val lengthPtr = alloc<DWORDVar>().ptr
            val incomingPacket = wintunReceivePacket.invoke(session, lengthPtr)
            if (incomingPacket != null) {
                val length = lengthPtr.pointed.value
                packet.usePinned {
                    memcpy(it.addressOf(offset), incomingPacket, length.convert())
                }
                wintunReleaseReceivePacket.invoke(session, incomingPacket)
                return length.convert()
            }
        }
        return 0
    }

    actual fun writePacket(packet: ByteArray, offset: Int): Int {
        val length = packet.size - offset
        val outgoingPacket = wintunAllocateSendPacket.invoke(session, length.convert())
        packet.usePinned {
            memcpy(outgoingPacket, it.addressOf(offset), length.convert())
            wintunSendPacket.invoke(session, outgoingPacket)
        }
        return length
    }

    actual fun close() {
        wintunEndSession.invoke(session)
        wintunCloseAdapter.invoke(adapter)
    }

    actual fun configureRouting() {
        val interfaceIndex = 0
        val deviceGateway = address.changeBit(2)
        runCmd("netsh interface ip set interface $interfaceIndex metric=0")
        runCmd("netsh interface ip set address $interfaceIndex static $address/24 gateway=$deviceGateway")
        runCmd("netsh interface ip set dnsservers $interfaceIndex static $dnsAddress register=primary validate=no")
        runCmd("netsh interface ip add route 0.0.0.0/1 $interfaceIndex $deviceGateway store=active")
        runCmd("netsh interface ip add route 128.0.0.0/1 $interfaceIndex $deviceGateway store=active")
    }

    private fun gatewayMac(): MacAddress {
        TODO()
    }

    actual companion object
}
