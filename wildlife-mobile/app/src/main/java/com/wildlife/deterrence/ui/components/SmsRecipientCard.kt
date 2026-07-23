package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmsRecipientCard(
    fullName: String,
    phoneNumber: String,
    relation: String,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relationLabel = when (relation.lowercase()) {
        "family", "gia đình" -> "Gia đình"
        "ranger", "kiểm lâm" -> "Kiểm lâm"
        "local_authority", "chính quyền" -> "Chính quyền"
        else -> relation
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val relationBgColor = when (relation.lowercase()) {
        "family", "gia đình" -> if (isDark) Color(0xFF0D47A1).copy(alpha = 0.3f) else Color(0xFFE3F2FD) // Blue
        "ranger", "kiểm lâm" -> if (isDark) Color(0xFF3E350E).copy(alpha = 0.5f) else Color(0xFFE8F5E9) // Vàng trong dark mode
        else -> if (isDark) Color(0xFFE65100).copy(alpha = 0.3f) else Color(0xFFFFF3E0) // Orange
    }

    val relationTextColor = when (relation.lowercase()) {
        "family", "gia đình" -> if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
        "ranger", "kiểm lâm" -> if (isDark) Color(0xFFD4AC0D) else Color(0xFF2E7D32) // Vàng trong dark mode
        else -> if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(relationBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = relationLabel,
                            color = relationTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phoneNumber,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa người nhận",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
