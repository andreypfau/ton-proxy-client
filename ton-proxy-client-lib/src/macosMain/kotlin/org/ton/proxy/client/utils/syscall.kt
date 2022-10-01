package org.ton.proxy.client.utils

import kotlinx.cinterop.*
import platform.posix.*

fun closeOnExec(fd: Int) =
    fcntl(fd, F_SETFD, FD_CLOEXEC)

fun setNonBlock(fd: Int, nonBlocking: Boolean) {
    var flag = fcntl(fd, F_GETFL, 0).check()
    flag = if (nonBlocking) {
        flag or O_NONBLOCK
    } else {
        flag and (O_NONBLOCK.inv())
    }
    fcntl(fd, F_SETFL, flag).check()
}

fun getSockoptString(fd: Int, level: Int, opt: Int) = memScoped {
    ByteArray(256).usePinned {
        val len = alloc<socklen_tVar> {
            value = 256u
        }
        getsockopt(fd, level, opt, it.addressOf(0), len.ptr).check()
        it.get().toKString(endIndex = len.value.convert())
    }
}
