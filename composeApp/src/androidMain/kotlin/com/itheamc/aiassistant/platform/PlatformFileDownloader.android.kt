package com.itheamc.aiassistant.platform

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.database.getIntOrNull
import androidx.core.net.toUri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformFileDownloader(private val context: Context) {

    val downloadManager by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager }

    actual fun download(
        url: String,
        fileName: String
    ): Flow<FileDownloadState> = callbackFlow {
        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle("Downloading $fileName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        if (downloadManager == null) {
            trySend(FileDownloadState.Error("Download Failed"))
            return@callbackFlow
        }

        val id = downloadManager!!.enqueue(request)

        var isDownloading = true

        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = downloadManager?.query(query)

            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status =
                    if (statusIndex >= 0) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {

                        val uriStringIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val uriString =
                            if (uriStringIndex >= 0) cursor.getString(uriStringIndex) else null

                        if (uriString != null) {
                            trySend(FileDownloadState.Success(uriString))
                        } else {
                            trySend(FileDownloadState.Error("Download Failed"))
                        }

                        isDownloading = false
                    }

                    DownloadManager.STATUS_RUNNING -> {
                        val bytesDownloadedSoFarColumnIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesDownloaded =
                            if (bytesDownloadedSoFarColumnIndex >= 0) cursor.getInt(
                                bytesDownloadedSoFarColumnIndex
                            ) else null

                        val totalSizeColumnIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val totalBytes =
                            if (totalSizeColumnIndex >= 0) cursor.getInt(totalSizeColumnIndex) else null

                        if (totalBytes != null && totalBytes > 0) {
                            val progress = (bytesDownloaded?.toFloat() ?: 0F) / totalBytes.toFloat()
                            trySend(FileDownloadState.InProgress(progress))
                        }
                    }

                    DownloadManager.STATUS_FAILED -> {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val errorCode =
                            if (statusIndex >= 0) cursor.getIntOrNull(reasonIndex) else null

                        val error = when (errorCode) {
                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
                            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
                            DownloadManager.ERROR_FILE_ERROR -> "File error"
                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
                            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
                            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Paused for Wi-Fi"
                            DownloadManager.PAUSED_UNKNOWN -> "Paused for unknown reason"
                            else -> "Download failed"
                        }

                        trySend(FileDownloadState.Error(error))
                        isDownloading = false
                    }
                }
            }
            cursor?.close()
            if (isDownloading) delay(250)
        }
        awaitClose { /* Cleanup if needed */ }
    }
}