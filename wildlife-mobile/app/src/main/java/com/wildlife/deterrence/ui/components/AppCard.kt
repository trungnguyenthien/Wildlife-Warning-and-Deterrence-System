package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isAlert: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    
    val containerColor = if (isAlert) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isAlert) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val borderStroke = BorderStroke(1.dp, borderColor)
    
    val elevationValue = if (onClick != null) 4.dp else 2.dp
    val cardElevation = CardDefaults.cardElevation(defaultElevation = elevationValue)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = borderStroke,
            elevation = cardElevation
        ) {
            Column(content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = borderStroke,
            elevation = cardElevation
        ) {
            Column(content = content)
        }
    }
}
