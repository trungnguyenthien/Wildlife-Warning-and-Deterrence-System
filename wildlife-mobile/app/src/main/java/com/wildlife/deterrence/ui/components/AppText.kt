package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
private fun AppTextWrapper(
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector?,
    textColor: Color,
    content: @Composable () -> Unit
) {
    if (leadingIcon != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = textColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            content()
        }
    } else {
        content()
    }
}

@Composable
fun AppTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    leadingIcon: ImageVector? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    val resolvedColor = color.takeOrElse { MaterialTheme.colorScheme.onBackground }
    AppTextWrapper(modifier = modifier, leadingIcon = leadingIcon, textColor = resolvedColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = resolvedColor,
            textAlign = textAlign,
            maxLines = maxLines
        )
    }
}

@Composable
fun AppSectionTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    leadingIcon: ImageVector? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    val resolvedColor = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    AppTextWrapper(modifier = modifier, leadingIcon = leadingIcon, textColor = resolvedColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = resolvedColor,
            textAlign = textAlign,
            maxLines = maxLines
        )
    }
}

@Composable
fun AppSubTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    leadingIcon: ImageVector? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    val resolvedColor = color.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) }
    AppTextWrapper(modifier = modifier, leadingIcon = leadingIcon, textColor = resolvedColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = resolvedColor,
            textAlign = textAlign,
            maxLines = maxLines
        )
    }
}

@Composable
fun AppBodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    leadingIcon: ImageVector? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    val resolvedColor = color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    AppTextWrapper(modifier = modifier, leadingIcon = leadingIcon, textColor = resolvedColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = resolvedColor,
            textAlign = textAlign,
            maxLines = maxLines
        )
    }
}
