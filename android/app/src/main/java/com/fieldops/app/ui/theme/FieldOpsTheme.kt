package com.fieldops.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1565C0)
val PrimaryDarkBlue = Color(0xFF0D47A1)
val AccentAmber = Color(0xFFFF8F00)
val HeaderBackgroundBlue = Color(0xFFE3F2FD)
val StatusPendingOrange = Color(0xFFE65100)
val StatusInProgressBlue = Color(0xFF1565C0)
val StatusCompletedGreen = Color(0xFF2E7D32)
val HighPriorityBackground = Color(0xFFFFF3E0)
val TextPrimaryColor = Color(0xFF212121)
val TextSecondaryColor = Color(0xFF757575)
val ErrorRedColor = Color(0xFFC62828)

private val FieldOpsColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentAmber,
    background = Color.White,
    surface = Color.White,
    onBackground = TextPrimaryColor,
    onSurface = TextPrimaryColor,
    error = ErrorRedColor,
)

@Composable
fun FieldOpsTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FieldOpsColorScheme,
        content = content,
    )
}
