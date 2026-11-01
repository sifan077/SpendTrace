package com.sifan.spendtrace.data.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 通过 SAF 读写用户选择的备份文件，不申请任何存储权限。 */
object BackupFiles {

    /** 读取文件内容；超过 [BackupService.MAX_BACKUP_BYTES] 时提前中止，避免大文件占用内存。 */
    suspend fun read(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw BackupException("无法读取该文件，请换一个文件试试。")
            stream.use { input ->
                val output = ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    output.write(chunk, 0, read)
                    if (output.size() > BackupService.MAX_BACKUP_BYTES) {
                        throw BackupException("备份文件过大，无法导入。")
                    }
                }
                output.toByteArray()
            }
        } catch (error: IOException) {
            throw BackupException("读取文件时出现问题，请重试。")
        }
    }

    /** 把文本写入用户选定的文件。 */
    suspend fun write(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw BackupException("无法写入该位置，请换一个目录试试。")
            stream.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
            }
        } catch (error: IOException) {
            throw BackupException("写入文件时出现问题，请检查设备剩余存储空间。")
        }
    }

    /** 读取用户看到的文件名，取不到时返回 null。 */
    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)
            }
    }.getOrNull()
}
