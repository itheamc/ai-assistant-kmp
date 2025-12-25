package com.itheamc.aiassistant.platform

import kotlinx.coroutines.flow.Flow

/**
 * A platform-specific utility class responsible for downloading files from a remote URL.
 *
 * As an `expect` class, its implementation is provided by each platform module (e.g., Android, iOS)
 * to handle platform-specific networking and file system APIs.
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformFileDownloader {

    /**
     * Initiates a file download from the specified URL and saves it with the given file name.
     *
     * @param url The remote URL of the file to be downloaded.
     * @param fileName The name to be assigned to the downloaded file.
     * @return A [Flow] emitting the current [FileDownloadState] of the operation, including
     * progress updates, success with the file path, or error details.
     */
    fun download(url: String, fileName: String): Flow<FileDownloadState>
}

/**
 * Sealed class representing the state of a download operation.
 * It can be one of three states: OnProgress, Success, or Error.
 */
sealed class FileDownloadState {

    /**
     * Represents the progress of the download.
     *
     * @property progress A float value indicating the percentage of the download completed (0.0 to 1.0).
     */
    data class InProgress(val progress: Float) : FileDownloadState()

    /**
     * Represents a successful download.
     *
     * @property path The downloaded file path.
     */
    data class Success(val path: String) : FileDownloadState()

    /**
     * Represents an error that occurred during the download.
     *
     * @property message A string containing the error message.
     */
    data class Error(val message: String) : FileDownloadState()
}