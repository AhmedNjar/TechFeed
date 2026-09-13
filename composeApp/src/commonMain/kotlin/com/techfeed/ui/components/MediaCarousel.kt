package com.techfeed.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.techfeed.data.model.MediaItem
import com.techfeed.ui.theme.TechFeedColors
import com.techfeed.ui.theme.TechFeedType

/**
 * A source-media gallery. It is intentionally independent of ContentRenderer:
 * feed media is extracted deterministically, while content blocks may be an
 * optional AI editorial summary.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCarousel(
    media: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    if (media.isEmpty()) return

    var lightboxIndex by remember { mutableStateOf<Int?>(null) }
    val pagerState = rememberPagerState(pageCount = { media.size })

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(TechFeedColors.Surface700),
        ) { page ->
            MediaPage(
                item = media[page],
                onImageClick = { lightboxIndex = page },
            )
        }

        if (media.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(media.size) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp, vertical = 4.dp)
                            .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (index == pagerState.currentPage) TechFeedColors.Accent else TechFeedColors.Surface600),
                    )
                }
            }
        }
    }

    lightboxIndex?.let { index ->
        val selected = media[index]
        if (selected is MediaItem.Image) {
            ImageLightbox(selected, onDismiss = { lightboxIndex = null })
        }
    }
}

@Composable
private fun MediaPage(item: MediaItem, onImageClick: () -> Unit) {
    when (item) {
        is MediaItem.Image -> AsyncImage(
            model = item.url,
            contentDescription = item.caption ?: "Article image — tap to zoom",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onImageClick),
        )
        is MediaItem.Video -> VideoPreview(item)
    }
}

@Composable
private fun VideoPreview(item: MediaItem.Video) {
    val uriHandler = LocalUriHandler.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (item.thumbnailUrl != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = "Video preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().background(TechFeedColors.Surface700))
        }
        IconButton(
            onClick = { uriHandler.openUri(item.streamUrl) },
            modifier = Modifier
                .align(Alignment.Center)
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.58f)),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Open video", tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Text(
            "Video",
            style = TechFeedType.Caption,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}

@Composable
private fun ImageLightbox(item: MediaItem.Image, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = Color.Black,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = item.url,
                    contentDescription = item.caption ?: "Zoomed article image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .transformable(transformState),
                )
            }
        },
    )
}
