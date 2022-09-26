package org.ton.proxy.client.device

import com.github.andreypfau.kotlinio.utils.PosixException
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.ton.proxy.client.utils.check
import os.File
import platform.osx.CTLIOCGINFO
import platform.osx.ctl_info
import platform.osx.sockaddr_ctl
import platform.posix.*
import unix.closeOnExec
import unix.getSockoptString
import kotlin.coroutines.CoroutineContext

actual class VirtualDevice(
    actual val file: File,
) : CoroutineScope {
    private val ioContext by lazy {
        newSingleThreadContext("I/O Tunnel ${toString()}")
    }
    private val job = Job()
    override val coroutineContext: CoroutineContext by lazy {
        job + ioContext
    }

    actual val name: String
        get() = getSockoptString(
            file.descriptor.fd,
            2, // SYSPROTO_CONTROL
            2, // UTUN_OPT_IFNAME
        )
    actual val mtu: Int
        get() = 1500
    private var routeSocket: Int = -1

    override fun toString(): String = "VirtualDevice($routeSocket, $name)"

    actual fun readPacket(buf: ByteArray, offset: Int): Int =
        buf.usePinned {
            read(file.descriptor.fd, it.addressOf(offset), (buf.size - offset).convert())
        }.toInt().check { it >= 0 || it == EAGAIN }

    actual fun writePacket(buf: ByteArray, offset: Int): Int {
        buf[offset] = 0
        buf[offset + 1] = 0
        buf[offset + 2] = 0
        if (buf[offset + 4].toUInt() shr 4 == 6u) {
            buf[3] = AF_INET6.toByte()
        } else {
            buf[3] = AF_INET.toByte()
        }
        return buf.usePinned {
            write(file.descriptor.fd, it.addressOf(offset), (buf.size - offset).convert())
        }.toInt()
    }

    actual fun flush() {
    }

    actual fun close() {
        file.close()
    }

    suspend fun routeListener(tunIfIndex: UInt): Unit = suspendCancellableCoroutine { coroutine ->
        val data = ByteArray(1024)
        while (coroutine.isActive) {
            val n = data.usePinned {
                read(routeSocket, it.addressOf(0), data.size.convert())
            }.toInt()
            if (n == -1) {
                if (errno == EINTR) continue
                else throw PosixException(errno)
            }
            if (n < 14) continue
            if (data[3].toUInt().toInt() != RTM_IFINFO) continue
            if (data[12].toUInt() != tunIfIndex) continue
        }
    }

    actual companion object {
        const val UTUN_CONTROL_NAME = "com.apple.net.utun_control"

        fun createDevice(name: String, mtu: Int = VirtualDevice.DEFAULT_MTU): VirtualDevice = memScoped {
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
            createDeviceFromFile(File(fd, ""), mtu)
        }

        fun createDeviceFromFile(file: File, mtu: Int = VirtualDevice.DEFAULT_MTU): VirtualDevice = try {
            val device = VirtualDevice(file)
            val name = device.name
            println("Created device: $name")
            val ifIndex = if_nametoindex(name)
            device.routeSocket = socketCloseExec(AF_ROUTE, SOCK_RAW, AF_UNSPEC).check()
            println("Index: $ifIndex")
            device
        } catch (e: Exception) {
            file.close()
            throw e
        }
    }
}

fun socketCloseExec(family: Int, sotype: Int, proto: Int): Int {
    val fd = socket(family, sotype, proto)
    println("create fd: $fd")
    if (fd > 0) {
        closeOnExec(fd)
    }
    return fd
}
