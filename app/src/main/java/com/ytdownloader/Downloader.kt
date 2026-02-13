package com.ytdownloader

import android.os.Environment
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object Downloader {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    suspend fun download(
        videoInfo: VideoInfo,
        format: DownloadFormat,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val sanitizedTitle = sanitizeFilename(videoInfo.title)
        
        when (format) {
            DownloadFormat.MP4 -> downloadVideo(videoInfo, sanitizedTitle, downloadsDir, onProgress)
            DownloadFormat.MP3 -> downloadAudio(videoInfo, sanitizedTitle, downloadsDir, onProgress)
        }
    }
    
    private suspend fun downloadVideo(
        videoInfo: VideoInfo,
        title: String,
        outputDir: File,
        onProgress: (Float) -> Unit
    ) {
        // Get best video stream with audio
        val videoStream = videoInfo.videoStreams.firstOrNull()
            ?: throw Exception("No video stream available")
        
        val outputFile = File(outputDir, "$title.mp4")
        downloadFile(videoStream.url, outputFile, onProgress)
    }
    
    private suspend fun downloadAudio(
        videoInfo: VideoInfo,
        title: String,
        outputDir: File,
        onProgress: (Float) -> Unit
    ) {
        // Get best audio stream
        val audioStream = videoInfo.audioStreams.firstOrNull()
            ?: throw Exception("No audio stream available")
        
        // Download the audio stream
        val tempFile = File(outputDir, "$title.temp")
        downloadFile(audioStream.url, tempFile) { progress ->
            onProgress(progress * 0.8f) // 80% for download
        }
        
        // Convert to MP3 using FFmpeg
        val outputFile = File(outputDir, "$title.mp3")
        
        onProgress(0.85f)
        
        val result = FFmpeg.execute("-i \"${tempFile.absolutePath}\" -vn -acodec libmp3lame -q:a 2 \"${outputFile.absolutePath}\" -y")
        
        // Clean up temp file
        tempFile.delete()
        
        if (result != Config.RETURN_CODE_SUCCESS) {
            // If FFmpeg fails, just rename (might already be playable)
            tempFile.renameTo(outputFile)
        }
        
        onProgress(1f)
    }
    
    private fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ) {
        val request = Request.Builder()
            .url(url)
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            )
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Download failed: ${response.code}")
            }
            
            val body = response.body ?: throw Exception("Empty response")
            val contentLength = body.contentLength()
            
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                
                body.byteStream().use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        if (contentLength > 0) {
                            onProgress(totalBytesRead.toFloat() / contentLength)
                        }
                    }
                }
            }
        }
        
        onProgress(1f)
    }
    
    private fun sanitizeFilename(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(100) // Limit length
    }
}
