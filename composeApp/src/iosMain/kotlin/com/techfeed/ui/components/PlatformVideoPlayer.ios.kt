package com.techfeed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformVideoPlayer(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier
) {
    // Simple fallback for iOS in this demo
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Video Player not implemented for iOS yet",
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}
