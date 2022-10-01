package org.ton.proxy.client.wintun

import kotlinx.cinterop.reinterpret
import platform.windows.GetProcAddress
import platform.windows.LOAD_LIBRARY_SEARCH_DEFAULT_DIRS
import platform.windows.LoadLibraryExW
import wintun.*

val wintun = requireNotNull(LoadLibraryExW("wintun.dll", null, LOAD_LIBRARY_SEARCH_DEFAULT_DIRS))
val wintunCreateAdapter = GetProcAddress(wintun, "WintunCreateAdapter")!!.reinterpret<WINTUN_CREATE_ADAPTER_FUNC>()
val wintunCloseAdapter = GetProcAddress(wintun, "WintunCloseAdapter")!!.reinterpret<WINTUN_CLOSE_ADAPTER_FUNC>()
val wintunStartSession = GetProcAddress(wintun, "WintunStartSession")!!.reinterpret<WINTUN_START_SESSION_FUNC>()
val wintunEndSession = GetProcAddress(wintun, "WintunEndSession")!!.reinterpret<WINTUN_END_SESSION_FUNC>()
val wintunReceivePacket = GetProcAddress(wintun, "WintunReceivePacket")!!.reinterpret<WINTUN_RECEIVE_PACKET_FUNC>()
val wintunReleaseReceivePacket =
    GetProcAddress(wintun, "WintunReleaseReceivePacket")!!.reinterpret<WINTUN_RELEASE_RECEIVE_PACKET_FUNC>()
val wintunAllocateSendPacket =
    GetProcAddress(wintun, "WintunAllocateSendPacket")!!.reinterpret<WINTUN_ALLOCATE_SEND_PACKET_FUNC>()
val wintunSendPacket = GetProcAddress(wintun, "WintunSendPacket")!!.reinterpret<WINTUN_SEND_PACKET_FUNC>()
