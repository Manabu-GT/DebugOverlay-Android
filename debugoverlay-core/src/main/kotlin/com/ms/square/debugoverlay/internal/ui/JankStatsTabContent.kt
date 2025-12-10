package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.FrameInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.data.model.StateJankCount
import com.ms.square.debugoverlay.internal.util.formatTimestamp
import kotlinx.coroutines.flow.Flow

// Colors matching Material 3 / existing patterns
private val JankGreen = Color(0xFF1E6F50)
private val JankYellow = Color(0xFF7C5800)
private val JankRed = Color(0xFFB3261E)
private val SeverityMild = Color(0xFFF59E0B)
private val SeverityModerate = Color(0xFFEA580C)
private val SeveritySevere = Color(0xFFDC2626)

private const val JANK_PERCENTAGE_GOOD = 1f
private const val JANK_PERCENTAGE_WARNING = 5f
private const val OVERRUN_MODERATE_MS = 20
private const val OVERRUN_SEVERE_MS = 30

@Composable
internal fun JankStatsTabContent(jankStatsFlow: Flow<JankStatsUiState>, modifier: Modifier = Modifier) {
  val state by jankStatsFlow.collectAsStateWithLifecycle(initialValue = JankStatsUiState.EMPTY)

  when {
    state.totalFrames == 0 -> JankStatsEmptyState()
    else -> {
      LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        item { JankSummaryCard(state) }

        if (state.jankyFrames > 0) {
          item { StateBreakdownCard(state.stateBreakdown) }
          item { JankyFramesCard(state.jankyFramesList) }
        } else {
          item { NoJankMessage() }
        }
      }
    }
  }
}

@Composable
private fun JankStatsEmptyState() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("\uD83D\uDCCA", fontSize = 48.sp) // 📊
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.debugoverlay_jankstats_waiting),
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.debugoverlay_jankstats_waiting_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun JankSummaryCard(state: JankStatsUiState) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      JankPercentageDisplay(state)
      Spacer(Modifier.height(20.dp))
      RecentFramesTimeline(state.recentFrameJanks)
      Spacer(Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(
          R.string.debugoverlay_jankstats_summary,
          state.totalFrames,
          state.jankyFrames,
          state.avgFrameDurationMs
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun JankPercentageDisplay(state: JankStatsUiState) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = "%.1f%%".format(state.jankPercentage.value),
      style = MaterialTheme.typography.displayMedium,
      color = when {
        state.jankPercentage.value < JANK_PERCENTAGE_GOOD -> JankGreen
        state.jankPercentage.value < JANK_PERCENTAGE_WARNING -> JankYellow
        else -> JankRed
      }
    )
    if (state.jankyFrames == 0 && state.totalFrames > 0) {
      Spacer(Modifier.width(8.dp))
      Text("\u2713", fontSize = 24.sp, color = JankGreen) // ✓
    }
  }
  Text(
    text = stringResource(R.string.debugoverlay_jankstats_janky_frames),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentFramesTimeline(recentFrameJanks: List<Boolean>) {
  FlowRow(
    modifier = Modifier.padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    recentFrameJanks.forEach { isJank ->
      Box(
        modifier = Modifier
          .size(8.dp)
          .background(
            color = if (isJank) JankRed else MaterialTheme.colorScheme.outlineVariant,
            shape = CircleShape
          )
      )
    }
  }
  Text(
    text = stringResource(R.string.debugoverlay_jankstats_last_n_frames, recentFrameJanks.size),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}

@Composable
private fun StateBreakdownCard(breakdown: List<StateJankCount>) {
  if (breakdown.isEmpty()) return

  Column {
    Text(
      text = stringResource(R.string.debugoverlay_jankstats_state_breakdown),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      tonalElevation = 1.dp
    ) {
      Column(modifier = Modifier.padding(vertical = 8.dp)) {
        breakdown.forEachIndexed { index, item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(
                    color = if (index == 0) JankRed else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                  )
              )
              Spacer(Modifier.width(12.dp))
              Text(
                text = item.state,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
              )
            }
            Text(
              text = stringResource(R.string.debugoverlay_jankstats_n_janks, item.count),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          if (index < breakdown.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun JankyFramesCard(frames: List<FrameInfo>) {
  if (frames.isEmpty()) return

  Column {
    Text(
      text = stringResource(R.string.debugoverlay_jankstats_janky_frames_list),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerLowest,
      tonalElevation = 1.dp
    ) {
      Column {
        frames.forEachIndexed { index, frame ->
          JankyFrameItem(frame)
          if (index < frames.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun JankyFrameItem(frame: FrameInfo) {
  val severityColor = when {
    !frame.isJank -> Color.Transparent
    (frame.overrunMs ?: 0) > OVERRUN_SEVERE_MS -> SeveritySevere
    (frame.overrunMs ?: 0) > OVERRUN_MODERATE_MS -> SeverityModerate
    else -> SeverityMild
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .drawBehind {
        if (frame.isJank) {
          drawRect(
            color = severityColor,
            topLeft = Offset.Zero,
            size = Size(4.dp.toPx(), size.height)
          )
        }
      }
      .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
  ) {
    Column {
      // Timestamp
      Text(
        text = formatTimestamp(frame.timestampMs),
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium
      )

      // Duration line
      Text(
        text = buildAnnotatedString {
          append("${frame.durationUiMs}ms total")
          frame.durationCpuMs?.let { append(" \u00B7 ${it}ms CPU") }
          frame.overrunMs?.let {
            append(" \u00B7 ")
            withStyle(SpanStyle(color = JankRed, fontWeight = FontWeight.Medium)) {
              append("+${it}ms over")
            }
          }
        },
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      // States
      if (frame.states.isNotEmpty()) {
        Text(
          text = frame.states.joinToString { "${it.first}=${it.second}" },
          style = MaterialTheme.typography.bodySmall,
          fontFamily = FontFamily.Monospace,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun NoJankMessage() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text("\uD83C\uDF89", fontSize = 32.sp) // 🎉
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.debugoverlay_jankstats_no_jank),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
