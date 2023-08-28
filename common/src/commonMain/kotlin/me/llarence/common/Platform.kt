package me.llarence.common

import androidx.compose.runtime.Composable
import java.io.File
import java.nio.file.Path

expect fun getSavePath(): String

// TODO: Add more error handling
@Composable
fun getFile(name: String): File {
    val file = Path.of(getSavePath(), name).toFile()

    if (!file.exists()) {
        file.createNewFile()
    }

    return file
}
