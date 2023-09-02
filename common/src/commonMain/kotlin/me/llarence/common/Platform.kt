package me.llarence.common

import androidx.compose.runtime.Composable
import java.io.File

@Composable
expect fun init()

expect fun getSavePath(): String

// TODO: Add more error handling
fun getFile(filename: String): File {
    val file = File(getSavePath(), filename)

    if (!file.exists()) {
        file.createNewFile()
    }

    return file
}
