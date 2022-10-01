package org.ton.proxy.client

import com.github.andreypfau.kotlinio.address.Inet4Address

@CName("ton_proxy_client_start")
expect fun tonProxyClientStart(address: Inet4Address, dnsAddress: Inet4Address)
