package com.opendedownloader.app.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.opendedownloader.app.db.AppDatabase
import com.opendedownloader.app.db.DownloadEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

interface DownloadListener {
    fun onProgress(downloadId: Long, downloadedBytes: Long, totalBytes: Long)
    fun onStatusChanged(downloadId: Long, status: String)
}

object DownloadEngine {
    private const val TAG = "DownloadEngine"
    private val executorService = Executors.newFixedThreadPool(4)
    private val activeTasks = ConcurrentHashMap<Long, DownloadRunnable>()
    private val listeners = ConcurrentHashMap.newKeySet<DownloadListener>()
    private val handler = Handler(Looper.getMainLooper())

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun addListener(listener: DownloadListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: DownloadListener) {
        listeners.remove(listener)
    }

    private fun notifyProgress(downloadId: Long, downloadedBytes: Long, totalBytes: Long) {
        handler.post {
            for (listener in listeners) {
                listener.onProgress(downloadId, downloadedBytes, totalBytes)
            }
        }
    }

    private fun notifyStatus(downloadId: Long, status: String) {
        handler.post {
            for (listener in listeners) {
                listener.onStatusChanged(downloadId, status)
            }
        }
    }

    /**
     * Enqueues a new download task.
     */
    fun startDownload(context: Context, url: String, fileName: String, destinationDir: File): Long {
        val db = AppDatabase.getInstance(context)
        val file = File(destinationDir, fileName)

        // Ensure destination dir exists
        destinationDir.mkdirs()

        // Create initial download entry
        val entity = DownloadEntity(
            url = url,
            fileName = fileName,
            filePath = file.absolutePath,
            status = "PENDING"
        )

        var insertedId: Long = 0
        val thread = Thread {
            insertedId = db.downloadDao().insert(entity)
            handler.post {
                resumeDownload(context, insertedId)
            }
        }
        thread.start()
        thread.join() // wait briefly for insertion

        return insertedId
    }

    /**
     * Resumes a paused download or starts a newly added download.
     */
    fun resumeDownload(context: Context, downloadId: Long) {
        synchronized(this) {
            if (activeTasks.containsKey(downloadId)) {
                Log.d(TAG, "Download $downloadId is already running.")
                return
            }
            val runnable = DownloadRunnable(context, downloadId)
            activeTasks[downloadId] = runnable
            executorService.execute(runnable)
        }
    }

    /**
     * Pauses an active download.
     */
    fun pauseDownload(context: Context, downloadId: Long) {
        synchronized(this) {
            val runnable = activeTasks[downloadId]
            if (runnable != null) {
                runnable.pause()
                activeTasks.remove(downloadId)
            } else {
                // If not active, just update status in DB to PAUSED
                val db = AppDatabase.getInstance(context)
                Executors.newSingleThreadExecutor().execute {
                    val entity = db.downloadDao().getById(downloadId)
                    if (entity != null && entity.status != "COMPLETED") {
                        db.downloadDao().update(entity.copy(status = "PAUSED"))
                        notifyStatus(downloadId, "PAUSED")
                    }
                }
            }
        }
    }

    /**
     * Cancels an active download and deletes the partially downloaded file.
     */
    fun cancelDownload(context: Context, downloadId: Long) {
        synchronized(this) {
            val runnable = activeTasks[downloadId]
            if (runnable != null) {
                runnable.cancel()
                activeTasks.remove(downloadId)
            }
            val db = AppDatabase.getInstance(context)
            Executors.newSingleThreadExecutor().execute {
                val entity = db.downloadDao().getById(downloadId)
                if (entity != null) {
                    db.downloadDao().delete(entity)
                    val file = File(entity.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                    notifyStatus(downloadId, "CANCELLED")
                }
            }
        }
    }

    fun isDownloading(downloadId: Long): Boolean {
        return activeTasks.containsKey(downloadId)
    }

    private class DownloadRunnable(
        private val context: Context,
        private val downloadId: Long
    ) : Runnable {
        @Volatile
        private var isPaused = false
        @Volatile
        private var isCancelled = false

        fun pause() {
            isPaused = true
        }

        fun cancel() {
            isCancelled = true
        }

        override fun run() {
            val db = AppDatabase.getInstance(context)
            var entity = db.downloadDao().getById(downloadId) ?: return

            if (entity.status == "COMPLETED") {
                notifyStatus(downloadId, "COMPLETED")
                return
            }

            db.downloadDao().update(entity.copy(status = "DOWNLOADING", errorMessage = null))
            notifyStatus(downloadId, "DOWNLOADING")

            val file = File(entity.filePath)
            var downloadedBytes = file.length()
            var totalBytes = entity.totalBytes

            var response: Response? = null
            var randomAccessFile: RandomAccessFile? = null
            var inputStream: java.io.InputStream? = null

            try {
                // Determine whether range request can be made
                val requestBuilder = Request.Builder().url(entity.url)
                if (downloadedBytes > 0) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                    Log.d(TAG, "Resuming download $downloadId from $downloadedBytes bytes")
                }

                val request = requestBuilder.build()
                response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    // Range request could fail if server doesn't support it, or existing file size mismatch
                    if (response.code == 416 || downloadedBytes > 0) {
                        Log.w(TAG, "Range request failed (code ${response.code}). Restarting download from scratch.")
                        file.delete()
                        downloadedBytes = 0
                        val retryResponse = okHttpClient.newCall(Request.Builder().url(entity.url).build()).execute()
                        if (!retryResponse.isSuccessful) {
                            throw IOException("Unexpected HTTP code: ${retryResponse.code}")
                        }
                        response.close()
                        response = retryResponse
                    } else {
                        throw IOException("Unexpected HTTP code: ${response.code}")
                    }
                }

                val body = response.body ?: throw IOException("Empty response body")
                val responseLength = body.contentLength()

                // If we started from 0, totalBytes is responseLength
                // If we started from L, and got partial response (206), totalBytes is downloadedBytes + responseLength
                if (response.code == 206) {
                    if (totalBytes <= 0) {
                        totalBytes = downloadedBytes + responseLength
                    }
                } else {
                    totalBytes = responseLength
                    downloadedBytes = 0
                    if (file.exists()) {
                        file.delete()
                    }
                }

                // Update totalBytes in DB
                entity = entity.copy(totalBytes = totalBytes)
                db.downloadDao().update(entity)

                randomAccessFile = RandomAccessFile(file, "rw")
                if (response.code == 206) {
                    randomAccessFile.seek(downloadedBytes)
                } else {
                    randomAccessFile.seek(0)
                }

                inputStream = body.byteStream()
                val buffer = ByteArray(1024 * 16)
                var bytesRead: Int
                var lastUpdateTimestamp = System.currentTimeMillis()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isPaused) {
                        Log.d(TAG, "Download $downloadId paused by user.")
                        entity = entity.copy(
                            status = "PAUSED",
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                        db.downloadDao().update(entity)
                        notifyStatus(downloadId, "PAUSED")
                        return
                    }
                    if (isCancelled) {
                        Log.d(TAG, "Download $downloadId cancelled by user.")
                        if (file.exists()) {
                            file.delete()
                        }
                        entity = entity.copy(
                            status = "CANCELLED",
                            downloadedBytes = 0,
                            totalBytes = totalBytes
                        )
                        db.downloadDao().update(entity)
                        notifyStatus(downloadId, "CANCELLED")
                        return
                    }

                    randomAccessFile.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTimestamp >= 400) { // Throttle DB & callback updates
                        entity = entity.copy(downloadedBytes = downloadedBytes)
                        db.downloadDao().update(entity)
                        notifyProgress(downloadId, downloadedBytes, totalBytes)
                        lastUpdateTimestamp = now
                    }
                }

                // Final progress update
                entity = entity.copy(downloadedBytes = downloadedBytes)
                db.downloadDao().update(entity)
                notifyProgress(downloadId, downloadedBytes, totalBytes)

                // Verify file size
                if (totalBytes > 0 && downloadedBytes != totalBytes) {
                    throw IOException("File download incomplete. Downloaded $downloadedBytes of $totalBytes bytes.")
                }

                // Calculate SHA-256
                notifyStatus(downloadId, "VERIFYING")
                val sha256Checksum = calculateSha256(file)

                // Download completed successfully
                entity = entity.copy(
                    status = "COMPLETED",
                    sha256 = sha256Checksum
                )
                db.downloadDao().update(entity)
                notifyStatus(downloadId, "COMPLETED")

            } catch (e: Exception) {
                Log.e(TAG, "Download error in ID $downloadId: ${e.message}", e)
                val status = if (isPaused) "PAUSED" else "FAILED"
                entity = entity.copy(
                    status = status,
                    downloadedBytes = downloadedBytes,
                    errorMessage = e.message
                )
                db.downloadDao().update(entity)
                notifyStatus(downloadId, status)
            } finally {
                activeTasks.remove(downloadId)
                try {
                    inputStream?.close()
                } catch (ignored: Exception) {}
                try {
                    randomAccessFile?.close()
                } catch (ignored: Exception) {}
                try {
                    response?.close()
                } catch (ignored: Exception) {}
            }
        }

        private fun calculateSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            val fis = java.io.FileInputStream(file)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            fis.close()
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        }
    }
}
