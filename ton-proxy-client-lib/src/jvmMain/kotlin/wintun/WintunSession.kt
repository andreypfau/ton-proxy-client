package wintun

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class WintunSession internal constructor(
    val adapter: WintunAdapter,
    private val pointer: Long
) {
    suspend fun receivePacket(byteArray: ByteArray): Int = coroutineScope {
        var length = 0
        while (isActive) {
            length = Wintun.receivePacket(pointer, byteArray)
            if (length == 0) delay(1) else break
        }
        length
    }

    fun sendPacket(packet: ByteArray) {
        Wintun.sendPacket(pointer, packet)
    }
}
