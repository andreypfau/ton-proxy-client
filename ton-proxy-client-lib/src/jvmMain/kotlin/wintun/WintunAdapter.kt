package wintun

import java.util.*

class WintunAdapter(
    val name: String,
    val tunnelName: String,
    val guid: UUID = UUID.nameUUIDFromBytes(name.encodeToByteArray())
) {
    private var pointer: Long = Wintun.createAdaptor(name, tunnelName, guid)

    fun startSession(capacity: Int = 0x400000): WintunSession {
        val sessionPointer = Wintun.startSession(pointer, capacity)
        return WintunSession(this, sessionPointer)
    }
}
