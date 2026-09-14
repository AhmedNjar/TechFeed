package com.techfeed.ui.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.techfeed.ui.theme.TechFeedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

@OptIn(UnstableApi::class)
@Composable
actual fun PlatformVideoPlayer(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier
) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var localPath by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    // Unique filename based on URL hash
    val fileName = remember(url) {
        val hash = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        "video_$hash.mp4"
    }

    val cacheFile = remember(fileName) { File(context.cacheDir, fileName) }

    LaunchedEffect(url) {
        if (cacheFile.exists()) {
            localPath = cacheFile.absolutePath
            return@LaunchedEffect
        }

        isDownloading = true
        try {
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection()
                connection.connect()
                val length = connection.contentLength
                val input = connection.getInputStream()
                val output = cacheFile.outputStream()

                val buffer = ByteArray(8 * 1024)
                var total = 0
                var read = input.read(buffer)
                while (read != -1) {
                    total += read
                    if (length > 0) {
                        downloadProgress = total.toFloat() / length.toFloat()
                    }
                    output.write(buffer, 0, read)
                    read = input.read(buffer)
                }
                output.close()
                input.close()
            }
            localPath = cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to streaming if download fails
            localPath = url
        } finally {
            isDownloading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val path = localPath
        if (path != null) {
            VideoPlayerView(path)
        } else if (isDownloading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    color = TechFeedColors.Accent,
                    trackColor = TechFeedColors.Surface600,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerView(path: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(path)
            setMediaItem(mediaItem)
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
