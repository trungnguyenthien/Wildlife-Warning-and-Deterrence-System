package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraStatusBadge(
    isOnline: Boolean,
    offlineDurationSeconds: Long,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val indicatorColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
    val textColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)

    val labelText = if (isOnline) {
        "Online"
    } else {
        if (offlineDurationSeconds >= 30) {
            "Offline (${offlineDurationSeconds}s)"
        } else {
            "Offline"
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = labelText,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
