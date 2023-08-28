package me.llarence.common

import androidx.compose.ui.unit.dp
import java.io.File

const val title = "Tasks"

val width = 1200.dp
val height = 900.dp

// TODO: Add windows and mac
// TODO: Add more error handling
fun createSaveDirectory(): String {
    val directory = File("~/.local/share/Tasks")

    if (!directory.exists()) {
        directory.mkdir()
    }

    return directory.path
}

val savePath = createSaveDirectory()
actual fun getSavePath(): String {
    return savePath
}
