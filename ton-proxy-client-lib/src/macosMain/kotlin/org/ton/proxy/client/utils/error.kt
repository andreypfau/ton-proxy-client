package org.ton.proxy.client.utils

import com.github.andreypfau.kotlinio.utils.PosixException
import platform.posix.errno

inline fun Int.check(condition: (Int) -> Boolean = { it >= 0 }): Int {
    if (!condition(this)) {
        throw PosixException(errno)
    }
    return this
}
