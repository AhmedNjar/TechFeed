package com.techfeed.ui.util

import androidx.compose.runtime.Composable

@Composable
actual fun CommonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS as it doesn't have a physical back button.
    // Standard iOS back gestures are handled by the navigation controller.
}
