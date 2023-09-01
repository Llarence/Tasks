package me.llarence.common

import androidx.compose.ui.unit.dp
import java.nio.file.Path

const val title = "Tasks"

val width = 1200.dp
val height = 900.dp

// TODO: Add windows and mac
// TODO: Add more error handling
val path: String = run {
    val directory = Path.of(System.getProperty("user.home"), ".local/share/tasks").toFile()
    if (!directory.exists()) {
        println(directory.mkdir())
    }
    directory.path
}

actual fun getSavePath(): String {
    return path
}
