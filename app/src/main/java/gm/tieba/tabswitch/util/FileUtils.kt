@file:JvmName("FileUtils")

package gm.tieba.tabswitch.util

import gm.tieba.tabswitch.XposedContext
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

fun copy(input: Any?, output: Any?) {
    val inputStream: InputStream = when (input) {
        is InputStream -> input
        is File -> FileInputStream(input)
        is FileDescriptor -> FileInputStream(input)
        is String -> FileInputStream(input)
        else -> throw IllegalArgumentException("unknown input type")
    }

    val outputStream: OutputStream = when (output) {
        is OutputStream -> output
        is File -> FileOutputStream(output)
        is FileDescriptor -> FileOutputStream(output)
        is String -> FileOutputStream(output)
        else -> throw IllegalArgumentException("unknown output type")
    }

    copy(inputStream, outputStream)
}

fun copy(inputStream: InputStream, outputStream: OutputStream) {
    inputStream.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }
}

fun copy(bb: ByteBuffer, output: Any?) {
    val outputStream: OutputStream = when (output) {
        is OutputStream -> output
        is File -> FileOutputStream(output)
        is FileDescriptor -> FileOutputStream(output)
        is String -> FileOutputStream(output)
        else -> throw IllegalArgumentException("unknown output type")
    }
    outputStream.use {
        it.write(bb.array())
    }
}

fun toByteBuffer(inputStream: InputStream): ByteBuffer {
    return ByteBuffer.wrap(inputStream.readBytes())
}

fun getExtension(bb: ByteBuffer): String {
    val chunk = String(bb.array(), 0, 6)
    return when {
        chunk.contains("GIF") -> "gif"
        chunk.contains("PNG") -> "png"
        else -> "jpeg"
    }.also {
        bb.rewind()
    }
}

fun getParent(path: String): String {
    return path.substring(0, path.lastIndexOf(File.separatorChar))
}

fun getAssetFileContent(filename: String?): String? {
    return try {
        filename?.let { name ->
            XposedContext.sAssetManager.open(name).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        }
    } catch (ignored: IOException) {
        null
    }
}