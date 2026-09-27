package com.miaouss90.tellocontroler.record

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileDescriptor
import java.io.OutputStream

/**
 * Saves recordings where the user can find them: Android 10+ through MediaStore (Movies / Pictures /
 * Download › TelloControler, visible in Gallery and Files, no permission needed); Android 8–9 in the app's
 * external files directory.
 */
class MediaStorage(private val context: Context) {
    companion object {
        const val FOLDER = "TelloControler"
    }

    /** An open, seekable file for MediaMuxer; [finish] publishes it (keep) or deletes it. */
    class Target(private val descriptor: ParcelFileDescriptor, private val onFinish: (keep: Boolean) -> Unit) {
        val fileDescriptor: FileDescriptor get() = descriptor.fileDescriptor

        fun finish(keep: Boolean) {
            runCatching { descriptor.close() }
            onFinish(keep)
        }
    }

    fun createVideo(name: String): Target =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), name, "video/mp4", Environment.DIRECTORY_MOVIES)
            val descriptor = context.contentResolver.openFileDescriptor(uri, "rw") ?: error("Cannot open $name")
            Target(descriptor) { keep -> if (keep) publish(uri) else context.contentResolver.delete(uri, null, null) }
        } else {
            val file = legacyFile(Environment.DIRECTORY_MOVIES, name)
            val mode = ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE
            Target(ParcelFileDescriptor.open(file, mode)) { keep -> if (!keep) file.delete() }
        }

    /** Returns a human-readable location. */
    fun savePhoto(bitmap: Bitmap, name: String): String = write(
        name = name,
        mime = "image/jpeg",
        directory = Environment.DIRECTORY_PICTURES,
    ) { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }

    fun saveFile(source: File, name: String, mime: String): String = write(
        name = name,
        mime = mime,
        directory = Environment.DIRECTORY_DOWNLOADS,
    ) { out -> source.inputStream().use { it.copyTo(out) } }

    private fun write(name: String, mime: String, directory: String, body: (OutputStream) -> Unit): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when (directory) {
                Environment.DIRECTORY_PICTURES -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val uri = insert(collection, name, mime, directory)
            try {
                context.contentResolver.openOutputStream(uri)?.use(body) ?: error("Cannot open $name")
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
            publish(uri)
            return "$directory/$FOLDER/$name"
        }
        val file = legacyFile(directory, name)
        file.outputStream().use(body)
        return file.absolutePath
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insert(collection: Uri, name: String, mime: String, directory: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return context.contentResolver.insert(collection, values) ?: error("MediaStore refused $name")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publish(uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    private fun legacyFile(directory: String, name: String): File {
        val dir = File(context.getExternalFilesDir(directory) ?: context.filesDir, FOLDER).apply { mkdirs() }
        return File(dir, name)
    }
}
