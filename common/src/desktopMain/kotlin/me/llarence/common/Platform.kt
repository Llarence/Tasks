package me.llarence.common

import java.net.URL

actual val platformName = "Desktop"

actual fun credURL(): URL {
    return object {}.javaClass.getResource("/secrets/cred.json")!!
}
