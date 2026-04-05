package com.example.techfeed.ui

// ركز: هنا بنستخدم الـ Preview الخاصة بأندرويد ستوديو مش بتاعت Jetbrains
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.techfeed.ui.screens.HomeScreenPreview

// تقدر هنا تحط showBackground وتتحكم في العرض براحتك
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AndroidHomeScreenPreview() {
    // بننادي على الـ Preview اللي كتبناها في commonMain
    HomeScreenPreview()
}