package me.llarence.common

import java.io.File
import java.nio.file.Path

expect fun getSavePath(): String

// TODO: Add more error handling
fun getFile(filename: String): File {
    val file = Path.of(getSavePath(), filename).toFile()

    if (!file.exists()) {
        file.createNewFile()
    }

    return file
}
