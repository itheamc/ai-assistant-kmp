package com.itheamc.aiassistant.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSUserDomainMask
import platform.darwin.NSObject

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformFileDownloader : NSObject(), NSURLSessionDownloadDelegateProtocol {

    private var callback: ((FileDownloadState) -> Unit)? = null
    private lateinit var fileName: String

    actual fun download(url: String, fileName: String): Flow<FileDownloadState> = callbackFlow {
        callback = { trySend(it) }
        this@PlatformFileDownloader.fileName = fileName

        val nsUrl = NSURL(string = url)
        val session = NSURLSession.sessionWithConfiguration(
            NSURLSessionConfiguration.defaultSessionConfiguration,
            delegate = this@PlatformFileDownloader,
            delegateQueue = NSOperationQueue.mainQueue
        )

        val task = session.downloadTaskWithURL(nsUrl)
        task.resume()
        awaitClose { callback = null }
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long
    ) {
        val progress = totalBytesWritten.toFloat() / totalBytesExpectedToWrite.toFloat()
        callback?.invoke(FileDownloadState.InProgress(progress))
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL
    ) {
        val fileManager = NSFileManager.defaultManager
        val dest =
            fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first() as NSURL
        val finalLocation = dest.URLByAppendingPathComponent(fileName)!!

        fileManager.moveItemAtURL(didFinishDownloadingToURL, finalLocation, null)
        callback?.invoke(FileDownloadState.Success(finalLocation.path!!))
    }
}