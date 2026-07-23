package com.wildlife.deterrence.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildlife.deterrence.R

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val logoBgColor = if (isDark) Color(0xFF6E5906) else Color(0xFF2C4C2C)

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(96.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(logoBgColor)
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Image(
        painter = painterResource(id = R.drawable.elephant),
        contentDescription = "Hệ thống Cảnh báo Động vật",
        modifier = Modifier.fillMaxSize()
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Wildlife Warning",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )
    Text(
      text = "Deterrence System",
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.secondary
    )
  }
}
