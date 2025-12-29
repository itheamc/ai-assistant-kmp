package com.itheamc.aiassistant.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUserDomainMask
import platform.darwin.NSObject

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformFileDownloader {

    actual fun download(url: String, fileName: String): Flow<FileDownloadState> = callbackFlow {
        val nsUrl = runCatching { NSURL(string = url) }.getOrNull() ?: run {
            trySend(FileDownloadState.Error("Invalid URL"))
            close()
            return@callbackFlow
        }

        val delegate = DownloadDelegate(
            fileName = fileName,
            onProgress = { progress ->
                trySend(FileDownloadState.InProgress(progress))
            },
            onComplete = { path ->
                path?.let {
                    trySend(FileDownloadState.Success(it))
                }
                close()
            },
            onError = { error ->
                trySend(FileDownloadState.Error(error))
                close()
            }
        )

        val configuration = NSURLSessionConfiguration.defaultSessionConfiguration
        val session = NSURLSession.sessionWithConfiguration(
            configuration = configuration,
            delegate = delegate,
            delegateQueue = NSOperationQueue.mainQueue
        )

        val downloadTask = session.downloadTaskWithURL(nsUrl)
        downloadTask.resume()

        awaitClose {
            downloadTask.cancel()
            session.invalidateAndCancel()
        }
    }
}

private class DownloadDelegate(
    private val fileName: String,
    private val onProgress: (Float) -> Unit,
    private val onComplete: (String?) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), NSURLSessionDownloadDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL
    ) {
        // 1. Get the file manager
        val fileManager = NSFileManager.defaultManager

        // 2. Get the Documents directory path
        val documentsPath = fileManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).first() as NSURL

        // 3. Create the destination URL with your fileName
        val destinationUrl = documentsPath.URLByAppendingPathComponent(fileName)!!

        try {
            // Remove existing file if it exists to avoid move errors
            if (fileManager.fileExistsAtPath(destinationUrl.path!!)) {
                fileManager.removeItemAtURL(destinationUrl, null)
            }

            // 4. Move the file from the .tmp path to your destination
            fileManager.moveItemAtURL(didFinishDownloadingToURL, destinationUrl, null)

            onComplete(destinationUrl.path)
        } catch (e: Exception) {
            onError("Failed to save file: ${e.message}")
        }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long
    ) {
        val progress = totalBytesWritten.toFloat() / totalBytesExpectedToWrite.toFloat()
        onProgress(progress)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        didCompleteWithError?.let {
            onError(it.localizedDescription)
        }
    }
}