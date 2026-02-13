package com.ytdownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ytdownloader.ui.theme.YTDownloaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Handle share intent
        val sharedUrl = when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extractYoutubeUrl(it) }
                } else null
            }
            else -> null
        }

        setContent {
            YTDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DownloaderScreen(initialUrl = sharedUrl)
                }
            }
        }
    }

    private fun extractYoutubeUrl(text: String): String? {
        val regex = Regex("""(https?://)?(www\.)?(youtube\.com/watch\?v=|youtu\.be/|youtube\.com/shorts/)[\w-]+""")
        return regex.find(text)?.value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(initialUrl: String? = null) {
    var url by remember { mutableStateOf(initialUrl ?: "") }
    var selectedFormat by remember { mutableStateOf(DownloadFormat.MP4) }
    var isLoading by remember { mutableStateOf(false) }
    var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloads by remember { mutableStateOf(listOf<DownloadItem>()) }

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YT Downloader", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF0000),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // URL Input Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Enter YouTube URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = url,
                            onValueChange = {
                                url = it
                                errorMessage = null
                                videoInfo = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://youtube.com/watch?v=...") },
                            singleLine = true,
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        clipboardManager.getText()?.text?.let { url = it }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, "Paste")
                                    }
                                    if (url.isNotEmpty()) {
                                        IconButton(onClick = {
                                            url = ""
                                            videoInfo = null
                                            errorMessage = null
                                        }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    }
                                }
                            }
                        )

                        // Format Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFormat == DownloadFormat.MP4,
                                onClick = { selectedFormat = DownloadFormat.MP4 },
                                label = { Text("MP4 Video") },
                                leadingIcon = if (selectedFormat == DownloadFormat.MP4) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedFormat == DownloadFormat.MP3,
                                onClick = { selectedFormat = DownloadFormat.MP3 },
                                label = { Text("MP3 Audio") },
                                leadingIcon = if (selectedFormat == DownloadFormat.MP3) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fetch Info Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        videoInfo = YouTubeExtractor.getVideoInfo(url)
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to fetch video info"
                                        videoInfo = null
                                    }
                                    isLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = url.isNotBlank() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isLoading) "Fetching..." else "Fetch Video Info")
                        }
                    }
                }
            }

            // Error Message
            errorMessage?.let { error ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Video Info Card
            videoInfo?.let { info ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                info.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                info.duration?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Timer, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(formatDuration(it), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                info.uploader?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val downloadItem = DownloadItem(
                                        id = System.currentTimeMillis().toString(),
                                        title = info.title,
                                        format = selectedFormat,
                                        progress = 0f,
                                        status = DownloadStatus.QUEUED
                                    )
                                    downloads = downloads + downloadItem

                                    scope.launch {
                                        try {
                                            val updatedItem = downloadItem.copy(status = DownloadStatus.DOWNLOADING)
                                            downloads = downloads.map { if (it.id == downloadItem.id) updatedItem else it }

                                            Downloader.download(
                                                videoInfo = info,
                                                format = selectedFormat,
                                                onProgress = { progress ->
                                                    downloads = downloads.map {
                                                        if (it.id == downloadItem.id) it.copy(progress = progress)
                                                        else it
                                                    }
                                                }
                                            )

                                            downloads = downloads.map {
                                                if (it.id == downloadItem.id) it.copy(
                                                    status = DownloadStatus.COMPLETED,
                                                    progress = 1f
                                                ) else it
                                            }
                                        } catch (e: Exception) {
                                            downloads = downloads.map {
                                                if (it.id == downloadItem.id) it.copy(
                                                    status = DownloadStatus.FAILED,
                                                    error = e.message
                                                ) else it
                                            }
                                        }
                                    }

                                    // Clear for next download
                                    url = ""
                                    videoInfo = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Download ${selectedFormat.name}")
                            }
                        }
                    }
                }
            }

            // Downloads Section
            if (downloads.isNotEmpty()) {
                item {
                    Text(
                        "Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(downloads.reversed()) { download ->
                    DownloadItemCard(download = download)
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(download: DownloadItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        when (download.format) {
                            DownloadFormat.MP4 -> Icons.Default.VideoFile
                            DownloadFormat.MP3 -> Icons.Default.AudioFile
                        },
                        null,
                        tint = when (download.status) {
                            DownloadStatus.COMPLETED -> Color(0xFF4CAF50)
                            DownloadStatus.FAILED -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        download.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    download.format.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (download.status) {
                DownloadStatus.QUEUED -> {
                    Text("Queued...", style = MaterialTheme.typography.bodySmall)
                }
                DownloadStatus.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = download.progress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${(download.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                DownloadStatus.COMPLETED -> {
                    Text(
                        "✓ Saved to Downloads",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
                DownloadStatus.FAILED -> {
                    Text(
                        "✗ ${download.error ?: "Download failed"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

enum class DownloadFormat { MP4, MP3 }
enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class DownloadItem(
    val id: String,
    val title: String,
    val format: DownloadFormat,
    val progress: Float,
    val status: DownloadStatus,
    val error: String? = null
)
