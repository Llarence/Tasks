package me.llarence.common

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// I hope this is not a problem
@SuppressLint("StaticFieldLeak")
lateinit var context: Context

@Composable
actual fun init() {
    context = LocalContext.current
}

actual fun getSavePath(): String {
    return context.filesDir.path
}
