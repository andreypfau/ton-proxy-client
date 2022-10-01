package org.ton.proxy.client.utils

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.popen

internal fun systemStr(command: String): String = memScoped {
    val file = popen(command, "r")
    val bufLength = 1024
    val buf = allocArray<ByteVar>(bufLength)
    val stringBuilder = StringBuilder()
    while (true) {
        val str = fgets(buf, bufLength, file)?.toKString() ?: break
        stringBuilder.append(str)
    }
    stringBuilder.toString()
}
