package com.techfeed.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformVideoPlayer(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
)
