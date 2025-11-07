package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MetricRow(
  label: String,
  value: String,
  statusColor: Color,
  lineGraphData: ImmutableList<Float>,
  lineGraphColor: Color,
  lineGraphMinValue: Float? = null,
  lineGraphMaxValue: Float? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .background(statusColor, CircleShape)
      )
      Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
      )
    }
    LineGraph(
      data = lineGraphData,
      color = lineGraphColor,
      modifier = Modifier.size(width = 40.dp, height = 16.dp),
      minValue = lineGraphMinValue,
      maxValue = lineGraphMaxValue
    )
    Text(
      text = value,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}
