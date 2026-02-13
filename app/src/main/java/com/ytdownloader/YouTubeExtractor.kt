package com.ytdownloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.stream.AudioStream

data class VideoInfo(
    val title: String,
    val uploader: String?,
    val duration: Long?,
    val thumbnailUrl: String?,
    val videoStreams: List<StreamOption>,
    val audioStreams: List<StreamOption>
)

data class StreamOption(
    val url: String,
    val format: String?,
    val quality: String?,
    val bitrate: Int?
)

object YouTubeExtractor {
    
    private var initialized = false
    
    private fun ensureInitialized() {
        if (!initialized) {
            NewPipe.init(DownloaderImpl.getInstance())
            initialized = true
        }
    }
    
    suspend fun getVideoInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        ensureInitialized()
        
        val normalizedUrl = normalizeUrl(url)
        val streamInfo = StreamInfo.getInfo(normalizedUrl)
        
        val videoStreams = streamInfo.videoStreams
            .filter { it.isVideoOnly.not() }  // Get streams with audio
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
            .map { stream ->
                StreamOption(
                    url = stream.content,
                    format = stream.format?.name,
                    quality = stream.resolution,
                    bitrate = stream.bitrate
                )
            }
        
        // Also get video-only streams for higher quality options
        val videoOnlyStreams = streamInfo.videoOnlyStreams
            .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
            .map { stream ->
                StreamOption(
                    url = stream.content,
                    format = stream.format?.name,
                    quality = "${stream.resolution} (video only)",
                    bitrate = stream.bitrate
                )
            }
        
        val audioStreams = streamInfo.audioStreams
            .sortedByDescending { it.averageBitrate }
            .map { stream ->
                StreamOption(
                    url = stream.content,
                    format = stream.format?.name,
                    quality = "${stream.averageBitrate}kbps",
                    bitrate = stream.averageBitrate
                )
            }
        
        VideoInfo(
            title = streamInfo.name,
            uploader = streamInfo.uploaderName,
            duration = streamInfo.duration,
            thumbnailUrl = streamInfo.thumbnails
                .maxByOrNull { it.width ?: 0 }
                ?.url
                ?: "",
            videoStreams = videoStreams + videoOnlyStreams,
            audioStreams = audioStreams
        )
    }
    
    private fun normalizeUrl(url: String): String {
        // Handle various YouTube URL formats
        val videoIdPattern = Regex("""(?:v=|youtu\.be/|shorts/)([a-zA-Z0-9_-]{11})""")
        val match = videoIdPattern.find(url)
        return if (match != null) {
            "https://www.youtube.com/watch?v=${match.groupValues[1]}"
        } else {
            url
        }
    }
}
