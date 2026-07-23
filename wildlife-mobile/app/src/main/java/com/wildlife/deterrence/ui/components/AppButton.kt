package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppButtonVariant { Filled, Outlined }
enum class AppButtonShape { Rounded, Capsule }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Filled,
    shape: AppButtonShape = AppButtonShape.Rounded,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val buttonShape = when (shape) {
        AppButtonShape.Capsule -> CircleShape
        AppButtonShape.Rounded -> RoundedCornerShape(12.dp)
    }

    val primaryGreen = Color(0xFF006400) // Project Dark Green

    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }

    if (variant == AppButtonVariant.Filled) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryGreen,
                contentColor = Color.White
            ),
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = buttonShape,
            border = BorderStroke(1.dp, primaryGreen),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = primaryGreen
            ),
            content = content
        )
    }
}
