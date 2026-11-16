package com.stashspace.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

class MediaDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "MediaDownloader"
        private const val DOWNLOAD_DIR = "Stashspace Downloads"
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private val downloadJobs = mutableMapOf<String, Job<*>>()
    
    data class DownloadStatus(
        val id: String,
        val url: String,
        val status: Status,
        val progress: Float = 0f,
        val filePath: String? = null,
        val error: String? = null
    ) {
        enum class Status {
            PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
        }
    }
    
    interface DownloadListener {
        fun onDownloadStart(id: String, url: String)
        fun onProgress(id: String, progress: Float)
        fun onDownloadComplete(id: String, filePath: String)
        fun onDownloadFailed(id: String, error: String)
        fun onDownloadCancelled(id: String)
    }
    
    fun downloadMedia(bookmark: Bookmark, listener: DownloadListener? = null): String {
        val job = scope.launch {
            val result = downloadBookmarkMedia(bookmark, listener)
            result
        }
        downloadJobs[bookmark.id] = job
        return bookmark.id
    }
    
    suspend fun downloadBookmarkMedia(bookmark: Bookmark, listener: DownloadListener? = null): String {
        try {
            val fileUrls = bookmark.mediaUrls
            if (fileUrls.isEmpty()) {
                listener?.onDownloadFailed(bookmark.id, "No media URLs")
                return bookmark.id
            }
            
            // Create download directory
            val downloadDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Download primary media
            val primaryUrl = fileUrls[0]
            val extension = getExtensionFromUrl(primaryUrl)
            val fileName = "${bookmark.id}_$extension"
            val filePath = File(downloadDir, fileName).absolutePath
            
            listener?.onDownloadStart(bookmark.id, primaryUrl)
            
            // Simulate download (in real app, would use actual download)
            for (i in 0..10) {
                delay(100)
                listener?.onProgress(bookmark.id, i / 10f)
            }
            
            // In a real implementation, you would:
            // 1. Use OkHttp or similar for actual download
            // 2. Handle different media types (video, image, carousel)
            // 3. Save to file
            // 4. Update database with local path
            
            // Create placeholder file for demo
            File(filePath).writeText("Downloaded: ${bookmark.caption}")
            
            listener?.onDownloadComplete(bookmark.id, filePath)
            return bookmark.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${bookmark.id}", e)
            listener?.onDownloadFailed(bookmark.id, e.message ?: "Unknown error")
            return bookmark.id
        }
    }
    
    fun cancelDownload(id: String) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
    }
    
    fun cancelAllDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }
    
    private fun getExtensionFromUrl(url: String): String {
        return try {
            url.substringAfterLast('.', "jpg")
                .substringBefore('?')
        } catch (e: Exception) {
            "jpg"
        }
    }
    
    fun getLocalMediaPath(bookmark: Bookmark): String? {
        val downloadDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
        val primaryUrl = bookmark.mediaUrls.firstOrNull() ?: return null
        val extension = getExtensionFromUrl(primaryUrl)
        return File(downloadDir, "${bookmark.id}_$extension").absolutePath
    }
    
    fun hasLocalMedia(bookmark: Bookmark): Boolean {
        val downloadDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
        val primaryUrl = bookmark.mediaUrls.firstOrNull() ?: return false
        val extension = getExtensionFromUrl(primaryUrl)
        return File(downloadDir, "${bookmark.id}_$extension").exists()
    }
}