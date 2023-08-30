package me.llarence.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getSavePath(): String {
    return LocalContext.current.filesDir.path
}
