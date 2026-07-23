package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun H1ChoiceButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F0F0))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF2C3E2C) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun V1ChoiceButtonGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionRightIcons: Map<String, ImageVector>? = null
) {
    val activeGreen = Color(0xFF2C4C2C)
    val activeBg = Color(0xFFEFF7EF)
    val inactiveBorder = Color.LightGray

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val rightIcon = optionRightIcons?.get(option)

            val borderStroke = if (isSelected) {
                BorderStroke(1.5.dp, activeGreen)
            } else {
                BorderStroke(1.dp, inactiveBorder)
            }

            val containerColor = if (isSelected) activeBg else Color.White

            OutlinedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
                border = borderStroke,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { onOptionSelected(option) }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Custom Radio circle indicator
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) activeGreen else Color.Transparent)
                            .then(
                                if (!isSelected) Modifier.background(Color.White) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color.Gray),
                                modifier = Modifier.size(22.dp),
                                content = {}
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Middle: Label
                    Text(
                        text = option,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) activeGreen else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Right: Optional Alarm warning icon
                    if (rightIcon != null) {
                        Icon(
                            imageVector = rightIcon,
                            contentDescription = null,
                            tint = activeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
