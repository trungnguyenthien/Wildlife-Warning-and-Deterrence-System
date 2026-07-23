package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun EventLogListItem(
    imageUrl: String,
    speciesName: String,
    confidence: Float,
    timestamp: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Determine resolved danger level color based on species name
    val speciesUpper = speciesName.uppercase()
    val isDangerous = speciesUpper in listOf("VOI", "HỔ", "BÁO", "TÊ GIÁC", "RẮN", "CÁ SẤU", "STRANGER", "NGƯỜI LẠ")
    val isMedium = speciesUpper in listOf("NAI", "NAI LỚN", "KHỈ", "KHỈ ĐÀN", "HEO RỪNG", "HEO")
    
    val badgeBgColor = when {
        isDangerous -> if (isDark) Color(0xFFC62828).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
        isMedium -> if (isDark) Color(0xFFEF6C00).copy(alpha = 0.2f) else Color(0xFFFFF3E0)
        else -> if (isDark) Color(0xFF3E350E).copy(alpha = 0.5f) else Color(0xFFE8F5E9)
    }
    
    val badgeTextColor = when {
        isDangerous -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
        isMedium -> if (isDark) Color(0xFFFFCC80) else Color(0xFFEF6C00)
        else -> if (isDark) Color(0xFFD4AC0D) else Color(0xFF2E7D32)
    }

    val confidencePercentage = (confidence * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Async Image Thumbnail
            AsyncImage(
                model = imageUrl,
                contentDescription = "Snapshot $speciesName",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Center: Info details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = speciesName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timestamp,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Confidence Badge / Warning Indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBgColor)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDangerous) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Nguy hiểm",
                        tint = badgeTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "$confidencePercentage%",
                    color = badgeTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
