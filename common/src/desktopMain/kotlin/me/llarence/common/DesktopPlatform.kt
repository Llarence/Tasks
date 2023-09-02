package me.llarence.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import java.io.File

const val title = "Tasks"

val width = 1200.dp
val height = 900.dp

// TODO: Add windows and mac
// TODO: Add more error handling
val path: String = run {
    val directory = File(System.getProperty("user.home"), ".local/share/tasks")
    if (!directory.exists()) {
        println(directory.mkdir())
    }
    directory.path
}

@Composable
actual fun init() {
}

actual fun getSavePath(): String {
    return path
}
