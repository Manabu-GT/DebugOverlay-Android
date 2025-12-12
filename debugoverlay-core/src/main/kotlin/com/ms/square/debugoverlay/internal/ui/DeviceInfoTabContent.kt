package com.ms.square.debugoverlay.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ms.square.debugoverlay.core.R
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.NetworkType
import com.ms.square.debugoverlay.internal.util.formatBytes
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

// Status indicator colors
private val StatusGoodColor = Color(0xFF4CAF50) // Green
private val WarningColor = Color(0xFFFF9800) // Orange

/**
 * Device Info tab showing comprehensive device information.
 *
 * @param deviceInfoFlow Flow of device information to collect and display.
 * @param modifier Modifier for the composable.
 */
@Suppress("LongMethod") // Declarative Compose UI - splitting would reduce readability
@Composable
internal fun DeviceInfoTabContent(deviceInfoFlow: Flow<DeviceInfo?>, modifier: Modifier = Modifier) {
  val deviceInfo by deviceInfoFlow.collectAsStateWithLifecycle(initialValue = null)

  deviceInfo?.let { info ->
    LazyColumn(
      modifier = modifier.fillMaxSize(),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Hardware Section
      item {
        SectionHeader(stringResource(R.string.debugoverlay_device_info_hardware))
      }

      // Device Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_device)) {
          InfoRow(stringResource(R.string.debugoverlay_device_info_manufacturer), info.hardware.manufacturer)
          InfoRow(stringResource(R.string.debugoverlay_device_info_model), info.hardware.model)
          InfoRow(stringResource(R.string.debugoverlay_device_info_brand), info.hardware.brand)
        }
      }

      // CPU Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_cpu)) {
          InfoRow(stringResource(R.string.debugoverlay_device_info_architecture), info.hardware.cpuArchitecture)
          InfoRow(stringResource(R.string.debugoverlay_device_info_cores), info.hardware.cpuCores.toString())
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_supported_abis),
            info.hardware.supportedAbis.joinToString(", ")
          )
        }
      }

      // Display Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_display)) {
          InfoRow(stringResource(R.string.debugoverlay_device_info_resolution), info.hardware.screenResolution)
          InfoRow(stringResource(R.string.debugoverlay_device_info_density), info.hardware.screenDensity)
          InfoRow(stringResource(R.string.debugoverlay_device_info_size_category), info.hardware.screenSizeCategory)
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_refresh_rate),
            stringResource(
              R.string.debugoverlay_device_info_hz_value,
              info.hardware.currentRefreshRate,
              info.hardware.maxRefreshRate
            )
          )
          InfoRow(stringResource(R.string.debugoverlay_device_info_opengl), info.hardware.openGlVersion)
        }
      }

      // Memory & Storage Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_memory_storage)) {
          // Total RAM
          InfoRow(stringResource(R.string.debugoverlay_device_info_total_ram), formatBytes(info.hardware.totalRam))

          // Available RAM with progress
          StorageIndicator(
            label = stringResource(R.string.debugoverlay_device_info_available_ram),
            available = info.hardware.availableRam,
            total = info.hardware.totalRam
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Total Storage
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_total_storage),
            formatBytes(info.hardware.totalStorage)
          )

          // Available Storage with progress
          StorageIndicator(
            label = stringResource(R.string.debugoverlay_device_info_available_storage),
            available = info.hardware.availableStorage,
            total = info.hardware.totalStorage
          )
        }
      }

      // System Section
      item {
        SectionHeader(stringResource(R.string.debugoverlay_device_info_system))
      }

      // Android Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_android)) {
          InfoRow(stringResource(R.string.debugoverlay_device_info_version), info.system.androidVersion)
          InfoRow(stringResource(R.string.debugoverlay_device_info_api_level), info.system.apiLevel.toString())
          InfoRow(stringResource(R.string.debugoverlay_device_info_security_patch), info.system.securityPatch)
        }
      }

      // Build Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_build)) {
          InfoRow(stringResource(R.string.debugoverlay_device_info_build_id), info.system.buildId)
          InfoRow(stringResource(R.string.debugoverlay_device_info_build_type), info.system.buildType)
          InfoRow(stringResource(R.string.debugoverlay_device_info_build_tags), info.system.buildTags)
          InfoRow(stringResource(R.string.debugoverlay_device_info_bootloader), info.system.bootloader)
          InfoRow(stringResource(R.string.debugoverlay_device_info_build_fingerprint), info.system.fingerprint)
        }
      }

      // Security & Services Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_security_services)) {
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_rooted),
            info.system.isRooted,
            positiveIsGood = false
          )
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_emulator),
            info.system.isEmulator,
            positiveIsGood = false
          )
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_installer),
            info.system.installerPackage ?: stringResource(R.string.debugoverlay_device_info_sideloaded)
          )
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_play_services),
            info.system.playServicesVersion ?: stringResource(R.string.debugoverlay_device_info_not_available)
          )
          if (info.system.playServicesVersionCode != null) {
            InfoRow(
              stringResource(R.string.debugoverlay_device_info_play_services_code),
              info.system.playServicesVersionCode.toString()
            )
          }
          InfoRow(stringResource(R.string.debugoverlay_device_info_kernel), info.system.kernelVersion)
        }
      }

      // Localization Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_localization)) {
          InfoRow(stringResource(R.string.debugoverlay_device_info_locale), info.system.locale)
          InfoRow(stringResource(R.string.debugoverlay_device_info_language), info.system.language)
          InfoRow(stringResource(R.string.debugoverlay_device_info_timezone), info.system.timeZone)
        }
      }

      // Battery Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_battery)) {
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_level),
            stringResource(R.string.debugoverlay_device_info_percentage_value, info.battery.level)
          )
          InfoRow(stringResource(R.string.debugoverlay_device_info_status), info.battery.status)
        }
      }

      // Hardware Features Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_hardware_features)) {
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_nfc),
            info.hardware.hardwareFeature.hasNfc
          )
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_bluetooth),
            info.hardware.hardwareFeature.hasBluetooth
          )
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_camera),
            info.hardware.hardwareFeature.hasCamera
          )
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_fingerprint),
            info.hardware.hardwareFeature.hasFingerprint
          )
        }
      }

      // Network Section
      item {
        SectionHeader(stringResource(R.string.debugoverlay_device_info_network))
      }

      // Connection Card
      item {
        InfoCard(title = stringResource(R.string.debugoverlay_device_info_connection)) {
          InfoRow(
            stringResource(R.string.debugoverlay_device_info_type),
            when (info.network.type) {
              NetworkType.WIFI -> stringResource(R.string.debugoverlay_device_info_wifi)
              NetworkType.CELLULAR -> stringResource(R.string.debugoverlay_device_info_cellular)
              NetworkType.ETHERNET -> stringResource(R.string.debugoverlay_device_info_ethernet)
              NetworkType.VPN -> stringResource(R.string.debugoverlay_device_info_vpn)
              NetworkType.NONE -> stringResource(R.string.debugoverlay_device_info_none)
            }
          )
          BooleanInfoRow(
            stringResource(R.string.debugoverlay_device_info_connected),
            info.network.isConnected
          )
        }
      }
    }
  } ?: Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(stringResource(R.string.debugoverlay_device_info_loading))
  }
}

/**
 * Section header for device info.
 */
@Composable
private fun SectionHeader(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
  )
}

/**
 * Info card container.
 */
@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
      )
      content()
    }
  }
}

/**
 * Info row with label and value.
 * Uses FlowRow for intelligent wrapping when values are too long.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoRow(label: String, value: String) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(end = 12.dp)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.Monospace,
      textAlign = androidx.compose.ui.text.style.TextAlign.End,
      modifier = Modifier.weight(1f, fill = false)
    )
  }
}

/**
 * Info row for boolean values with status dot indicator.
 * @param positiveIsGood If true, shows green for true/red for false. If false, shows red for true/green for false.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BooleanInfoRow(label: String, value: Boolean, positiveIsGood: Boolean = true) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(end = 12.dp)
    )
    Row(
      modifier = Modifier.weight(1f, fill = false),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val isGoodState = if (positiveIsGood) value else !value
      Box(
        modifier = Modifier
          .size(8.dp)
          .background(
            color = if (isGoodState) StatusGoodColor else MaterialTheme.colorScheme.error,
            shape = CircleShape
          )
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (value) {
          stringResource(R.string.debugoverlay_device_info_yes)
        } else {
          stringResource(R.string.debugoverlay_device_info_no)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace,
        textAlign = androidx.compose.ui.text.style.TextAlign.End
      )
    }
  }
}

/**
 * Storage indicator with progress bar.
 */
@Composable
private fun StorageIndicator(label: String, available: Long, total: Long) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = formatBytes(available),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Monospace
      )
    }

    // Progress bar showing free space ratio
    val progress = if (total > 0) (available.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    @Suppress("MagicNumber") // Standard percentage calculation
    val percentage = (progress * 100).roundToInt()

    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp),
      color = when {
        percentage < 10 -> MaterialTheme.colorScheme.error // Critical: < 10% free
        percentage < 25 -> WarningColor // Low: 10-25% free
        else -> MaterialTheme.colorScheme.primary // Good: > 25% free
      },
      trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    )

    Text(
      text = stringResource(R.string.debugoverlay_device_info_percentage_value, percentage),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
