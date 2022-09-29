package org.ton.proxy.client.handler

import com.github.andreypfau.kotlinio.packet.ipv4.IpV4Packet
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class IpDefragmentator(
    val duration: Duration,
    coroutineContext: CoroutineContext
) : CoroutineScope {
    private val fragmentedV4Timestamps = HashMap<UShort, Instant>()
    private val fragmentedV4Packets = HashMap<UShort, MutableList<IpV4Packet>>()

    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineName("IP Defragmentator")
    val job = launch {
        while (isActive) {
            delay(duration)
            gc(duration)
        }
    }.invokeOnCompletion {
        fragmentedV4Timestamps.clear()
        fragmentedV4Packets.clear()
    }

    operator fun invoke(iPv4Packet: IpV4Packet?): IpV4Packet? {
        return iPv4Packet // TODO: fix defragmentation
//        if (iPv4Packet == null) return null
//        return if (!iPv4Packet.header.isFragmented) {
//            iPv4Packet
//        } else {
//            val id = iPv4Packet.header.identification
//            fragmentedV4Timestamps[id] = Clock.System.now()
//            val fragments = fragmentedV4Packets.getOrPut(id) {
//                ArrayList()
//            }
//            fragments.add(iPv4Packet)
//            gc(duration)
//            if (!iPv4Packet.header.moreFragments) {
//                fragmentedV4Packets.remove(id)
//                fragmentedV4Timestamps.remove(id)
//                IPv4Packet.defragment(iPv4Packet)
//            } else {
//                null
//            }
//        }
    }

    fun gc(duration: Duration) {
        val now = Clock.System.now()
        fragmentedV4Timestamps.toList().forEach { (id, timestamp) ->
            if (now - timestamp > duration) {
                fragmentedV4Timestamps.remove(id)
                val packets = fragmentedV4Packets.remove(id)
                println("GC for IPv4 id: $id (${packets?.size ?: 0} packets)")
            }
        }
    }
}
