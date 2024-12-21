package com.nexomc.nexo.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun File.resolve(vararg path: String): File {
    return path.fold(this) { file, segment -> File(file, segment) }
}

fun Path.resolve(vararg path: String): Path {
    return path.fold(this) { file, segment -> file.resolve(segment) }
}

object FileUtils {
    @JvmStatic
    fun setHidden(path: Path) {
        runCatching {
            if (System.getProperty("os.name").startsWith("Windows")) Files.setAttribute(path, "dos:hidden", true)
            Files.isHidden(path)
        }.printOnFailure()
    }
}
