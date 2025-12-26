package com.itheamc.aiassistant.platform

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSError
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
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
    private val onProgress: (Float) -> Unit,
    private val onComplete: (String?) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), NSURLSessionDownloadDelegateProtocol {

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL
    ) {
        onComplete(didFinishDownloadingToURL.path)
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

//@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
//actual class PlatformFileDownloader {
//
//    @OptIn(ExperimentalForeignApi::class)
//    actual fun download(url: String, fileName: String): Flow<FileDownloadState> = callbackFlow {
//        val nsUrl = NSURL(string = url)
//        val session = NSURLSession.sharedSession
//
//        val downloadTask = session.downloadTaskWithURL(nsUrl) { location, response, error ->
//            if (error != null) {
//                trySend(FileDownloadState.Error(error.localizedDescription))
//            } else if (location != null) {
//                val fileManager = NSFileManager.defaultManager
//                val documentsPath = fileManager.URLsForDirectory(
//                    NSDocumentDirectory,
//                    NSUserDomainMask
//                ).first() as NSURL
//
//                val destinationURL = documentsPath.URLByAppendingPathComponent(fileName)
//
//                try {
//                    // Remove existing file if any
//                    if (fileManager.fileExistsAtPath(destinationURL!!.path!!)) {
//                        fileManager.removeItemAtURL(destinationURL, null)
//                    }
//
//                    // Move downloaded file
//                    fileManager.moveItemAtURL(location, destinationURL, null)
//                    trySend(FileDownloadState.Success(destinationURL.path!!))
//                } catch (e: Exception) {
//                    trySend(FileDownloadState.Error(e.message ?: "Failed to save file"))
//                }
//            }
//            close()
//        }
//
//        downloadTask.resume()
//
//        awaitClose {
//            downloadTask.cancel()
//        }
//    }
//}