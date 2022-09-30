package org.ton.proxy.client.app

object AppVersion {
    val VALUE = Thread.currentThread().contextClassLoader.getResource("version")
        ?.readText()?.split("\n")?.firstOrNull()
        ?: "UNKNOWN"

    override fun toString(): String = VALUE
}
