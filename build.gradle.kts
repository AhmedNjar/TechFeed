plugins {
    // بنعرف الـ Plugins هنا بـ apply false عشان نسمعها في الـ Modules التانية
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false

    // SQLDelight Plugin - المنقذ من جحيم الـ KSP
    alias(libs.plugins.sqldelight) apply false
}