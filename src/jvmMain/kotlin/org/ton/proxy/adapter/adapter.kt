//@file:Suppress("OPT_IN_USAGE")
//
//package org.ton.proxy.adapter
//
//import io.ktor.network.selector.*
//import io.ktor.network.sockets.*
//import io.ktor.util.*
//import io.ktor.utils.io.core.*
//import kotlinx.coroutines.*
//import org.pcap4j.packet.IpSelector
//import org.ton.proxy.client.handler.IpHandler
//import org.ton.proxy.utils.findBestInterface
//import wintun.WintunAdapter
//import java.net.*
//import kotlin.concurrent.thread
//
//suspend fun main() {
//    val inetAddress = findBestInterface() ?: error("Can't find best interface")
//    val adapterAddress = Inet4Address.getByName("10.8.0.2") as Inet4Address
//    val virtualAdapter = WintunAdapter("The Open Network", "ADNL")
//    val session = virtualAdapter.startSession()
//    println("inetAddress: $inetAddress")
//    val ipHandler = IpHandler(inetAddress, adapterAddress, session)
//    setupAdapter()
//    println("running!")
//    thread {
//        val buf = ByteArray(0x400000)
//        while (true) {
//            val length = runBlocking {
//                ipHandler.session.receivePacket(buf)
//            }
//            val packet = IpSelector.newPacket(buf, 0, length)
//            ipHandler.handleLocalPacket(packet)
//        }
//    }
//}
//
//private fun setupAdapter() {
//    val dnsServer = "1.1.1.1"
//    val address = "10.8.0.2"
//    val gateway = "10.8.0.1"
//    val interfaceIndex = 59
//    val routes = buildList {
//        add("netsh interface ip set interface $interfaceIndex metric=0 mtu=1500")
//        add("netsh interface ip set address $interfaceIndex static $address/24 gateway=$gateway")
//        add("netsh interface ip set dnsservers $interfaceIndex static $dnsServer register=primary validate=no")
//        add("netsh interface ip add route 0.0.0.0/1 $interfaceIndex $gateway store=active")
//        add("netsh interface ip add route 128.0.0.0/1 $interfaceIndex $gateway store=active")
//    }
//    routes.forEach { cmd ->
//        println("Running: $cmd")
//        Runtime.getRuntime().exec(cmd)
//    }
//}
