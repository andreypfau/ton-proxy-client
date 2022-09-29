package org.ton.proxy.client.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState

fun application() {
    System.clearProperty("skiko.library.path")
    androidx.compose.ui.window.application {
        var isOpen by remember { mutableStateOf(true) }
        if (isOpen) {
            val icon = painterResource("ton_symbol.png")
            val trayState = rememberTrayState()
            Tray(
                state = trayState,
                icon = icon,
                menu = {
                    Item(
                        text = "TON Proxy Client v${AppVersion}",
                        enabled = false,
                        onClick = {}
                    )
                    Item(
                        text = "Exit",
                        onClick = {
                            isOpen = false
                        }
                    )
                }
            )
        }
    }
}
