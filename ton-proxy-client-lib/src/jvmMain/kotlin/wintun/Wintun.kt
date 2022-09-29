package wintun

import java.util.*

object Wintun {
    init {
        System.loadLibrary("ton_proxy_client")
    }

    @JvmStatic
    external fun createAdaptor(name: String, tunnelName: String, guid: UUID): Long

    @JvmStatic
    external fun startSession(adaptor: Long, capacity: Int): Long

    @JvmStatic
    external fun receivePacket(session: Long, packet: ByteArray): Int

    @JvmStatic
    external fun sendPacket(session: Long, packet: ByteArray)
}

//@OptIn(ExperimentalUnsignedTypes::class)
//fun main() {
//    val adaptor = Wintun.createAdaptor(
//        "The Open Network", "ADNL", UUID.nameUUIDFromBytes("The Open Network".encodeToByteArray())
//    )
//    val session = Wintun.startSession(adaptor, 0x800000)
//    while (true) {
//        val packet = Wintun.receivePacket(session).toUByteArray()
//        when(val version = packet[0].toInt() shr 4) {
//            4 -> {
//                val src = "${packet[12]}.${packet[13]}.${packet[14]}.${packet[15]}"
//                val dst = "${packet[16]}.${packet[17]}.${packet[18]}.${packet[19]}"
//                val protocol = packet[9].toInt()
//                println("Received IPv$version proto $protocol packet: $src -> $dst")
//            }
//            6 -> {
//
//            }
//            else -> {
//                println("Received packet that was not IP")
//            }
//        }
//    }
//}
