/* SPDX-License-Identifier: GPL-2.0
 *
 * Copyright (C) 2018-2021 WireGuard LLC. All Rights Reserved.
 */

#include <winsock2.h>
#include <Windows.h>
#include <ws2ipdef.h>
#include <iphlpapi.h>
#include <mstcpip.h>
#include <winternl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include "wintun.h"

#pragma comment(lib, "IPHLPAPI.lib")

static WINTUN_CREATE_ADAPTER_FUNC *WintunCreateAdapter;
static WINTUN_CLOSE_ADAPTER_FUNC *WintunCloseAdapter;
static WINTUN_OPEN_ADAPTER_FUNC *WintunOpenAdapter;
static WINTUN_GET_ADAPTER_LUID_FUNC *WintunGetAdapterLUID;
static WINTUN_GET_RUNNING_DRIVER_VERSION_FUNC *WintunGetRunningDriverVersion;
static WINTUN_DELETE_DRIVER_FUNC *WintunDeleteDriver;
static WINTUN_SET_LOGGER_FUNC *WintunSetLogger;
static WINTUN_START_SESSION_FUNC *WintunStartSession;
static WINTUN_END_SESSION_FUNC *WintunEndSession;
static WINTUN_GET_READ_WAIT_EVENT_FUNC *WintunGetReadWaitEvent;
static WINTUN_RECEIVE_PACKET_FUNC *WintunReceivePacket;
static WINTUN_RELEASE_RECEIVE_PACKET_FUNC *WintunReleaseReceivePacket;
static WINTUN_ALLOCATE_SEND_PACKET_FUNC *WintunAllocateSendPacket;
static WINTUN_SEND_PACKET_FUNC *WintunSendPacket;

static HMODULE
InitializeWintun(void)
{
    HMODULE Wintun =
        LoadLibraryExW(L"wintun.dll", NULL, LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
    if (!Wintun)
        return NULL;
#define X(Name) ((*(FARPROC *)&Name = GetProcAddress(Wintun, #Name)) == NULL)
    if (X(WintunCreateAdapter) || X(WintunCloseAdapter) || X(WintunOpenAdapter) || X(WintunGetAdapterLUID) ||
        X(WintunGetRunningDriverVersion) || X(WintunDeleteDriver) || X(WintunSetLogger) || X(WintunStartSession) ||
        X(WintunEndSession) || X(WintunGetReadWaitEvent) || X(WintunReceivePacket) || X(WintunReleaseReceivePacket) ||
        X(WintunAllocateSendPacket) || X(WintunSendPacket))
#undef X
    {
        DWORD LastError = GetLastError();
        FreeLibrary(Wintun);
        SetLastError(LastError);
        return NULL;
    }
    return Wintun;
}

static USHORT
IPChecksum(_In_reads_bytes_(Len) BYTE *Buffer, _In_ DWORD Len)
{
    ULONG Sum = 0;
    for (; Len > 1; Len -= 2, Buffer += 2)
        Sum += *(USHORT *)Buffer;
    if (Len)
        Sum += *Buffer;
    Sum = (Sum >> 16) + (Sum & 0xffff);
    Sum += (Sum >> 16);
    return (USHORT)(~Sum);
}

static void
MakeICMP(_Out_writes_bytes_all_(28) BYTE Packet[28])
{
    memset(Packet, 0, 28);
    Packet[0] = 0x45;
    *(USHORT *)&Packet[2] = htons(28);
    Packet[8] = 255;
    Packet[9] = 1;
    *(ULONG *)&Packet[12] = htonl((10 << 24) | (6 << 16) | (7 << 8) | (8 << 0)); /* 10.6.7.8 */
    *(ULONG *)&Packet[16] = htonl((10 << 24) | (6 << 16) | (7 << 8) | (7 << 0)); /* 10.6.7.7 */
    *(USHORT *)&Packet[10] = IPChecksum(Packet, 20);
    Packet[20] = 8;
    *(USHORT *)&Packet[22] = IPChecksum(&Packet[20], 8);
    Log(WINTUN_LOG_INFO, L"Sending IPv4 ICMP echo request to 10.6.7.8 from 10.6.7.7");
}

static DWORD GetRunningDriverVersion(void) {
    return WintunGetRunningDriverVersion();
}
