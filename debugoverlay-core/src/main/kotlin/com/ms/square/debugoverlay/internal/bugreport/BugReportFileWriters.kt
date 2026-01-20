package com.ms.square.debugoverlay.internal.bugreport

import com.ms.square.debugoverlay.internal.data.model.AppExitInfo
import com.ms.square.debugoverlay.internal.data.model.DeviceInfo
import com.ms.square.debugoverlay.internal.data.model.JankStatsUiState
import com.ms.square.debugoverlay.internal.util.formatBytesFromKb
import com.ms.square.debugoverlay.internal.util.formatFullTimestamp
import com.ms.square.debugoverlay.model.LogEntry
import com.ms.square.debugoverlay.model.NetworkRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File

private const val SEPARATOR_WIDTH = 80

/**
 * Utility functions for writing bug report data to files.
 *
 * File format strategy:
 * - JSON for structured data: logs, network requests, device info, jank stats
 * - Plain text for narrative data: app exits, UI hierarchy
 */
internal object BugReportFileWriters {

  // Compact JSON - HTML report provides human-readable view
  private val json = Json { prettyPrint = false }

  // ============================================================================
  // JSON Writers (structured data)
  // ============================================================================

  /**
   * Writes logcat log entries to a JSON file.
   */
  fun writeLogcatLogs(logs: List<LogEntry>, file: File) {
    writeLogs(logs, "Logcat", file)
  }

  /**
   * Writes custom log source entries to a JSON file.
   */
  fun writeCustomLogs(logs: List<LogEntry>, sourceName: String, file: File) {
    writeLogs(logs, sourceName, file)
  }

  /**
   * Writes log entries to a JSON file with source name metadata.
   * Format: `{"sourceName": "...", "entries": [...]}`
   */
  private fun writeLogs(logs: List<LogEntry>, sourceName: String, file: File) {
    val jsonObj = buildJsonObject {
      put("sourceName", sourceName)
      put("entries", json.encodeToJsonElement(logs))
    }
    file.writeText(json.encodeToString(jsonObj))
  }

  /**
   * Writes network requests to a JSON file.
   */
  fun writeNetworkRequests(requests: List<NetworkRequest>, file: File) {
    file.writeText(json.encodeToString(requests))
  }

  /**
   * Writes device information to a JSON file using buildJsonObject.
   */
  @Suppress("LongMethod") // Length is from data fields, not complexity
  fun writeDeviceInfo(deviceInfo: DeviceInfo, file: File) {
    val jsonObj = buildJsonObject {
      putJsonObject("hardware") {
        put("manufacturer", deviceInfo.hardware.manufacturer)
        put("model", deviceInfo.hardware.model)
        put("brand", deviceInfo.hardware.brand)
        put("cpuArchitecture", deviceInfo.hardware.cpuArchitecture)
        put("cpuCores", deviceInfo.hardware.cpuCores)
        putJsonArray("supportedAbis") {
          deviceInfo.hardware.supportedAbis.forEach { add(JsonPrimitive(it)) }
        }
        put("screenSizeCategory", deviceInfo.hardware.screenSizeCategory)
        put("screenDensity", deviceInfo.hardware.screenDensity)
        put("screenResolution", deviceInfo.hardware.screenResolution)
        put("currentRefreshRate", deviceInfo.hardware.currentRefreshRate)
        put("maxRefreshRate", deviceInfo.hardware.maxRefreshRate)
        put("openGlVersion", deviceInfo.hardware.openGlVersion)
        put("totalRam", deviceInfo.hardware.totalRam)
        put("availableRam", deviceInfo.hardware.availableRam)
        put("totalStorage", deviceInfo.hardware.totalStorage)
        put("availableStorage", deviceInfo.hardware.availableStorage)
        putJsonObject("hardwareFeatures") {
          put("hasNfc", deviceInfo.hardware.hardwareFeature.hasNfc)
          put("hasBluetooth", deviceInfo.hardware.hardwareFeature.hasBluetooth)
          put("hasCamera", deviceInfo.hardware.hardwareFeature.hasCamera)
          put("hasFingerprint", deviceInfo.hardware.hardwareFeature.hasFingerprint)
        }
      }
      putJsonObject("battery") {
        put("level", deviceInfo.battery.level)
        put("status", deviceInfo.battery.status)
      }
      putJsonObject("system") {
        put("androidVersion", deviceInfo.system.androidVersion)
        put("apiLevel", deviceInfo.system.apiLevel)
        put("securityPatch", deviceInfo.system.securityPatch)
        put("buildId", deviceInfo.system.buildId)
        put("buildType", deviceInfo.system.buildType)
        put("buildTags", deviceInfo.system.buildTags)
        put("bootloader", deviceInfo.system.bootloader)
        put("fingerprint", deviceInfo.system.fingerprint)
        put("kernelVersion", deviceInfo.system.kernelVersion)
        put("uptimeMs", deviceInfo.system.uptimeMs)
        put("isRooted", deviceInfo.system.isRooted)
        put("isEmulator", deviceInfo.system.isEmulator)
        put("playServicesVersion", deviceInfo.system.playServicesVersion)
        put("playServicesVersionCode", deviceInfo.system.playServicesVersionCode)
        put("locale", deviceInfo.system.locale)
        put("language", deviceInfo.system.language)
        put("timeZone", deviceInfo.system.timeZone)
      }
      putJsonObject("network") {
        put("type", deviceInfo.network.type.name)
        put("isConnected", deviceInfo.network.isConnected)
      }
    }

    file.writeText(json.encodeToString(jsonObj))
  }

  /**
   * Writes JankStats data to a JSON file using buildJsonObject.
   */
  fun writeJankStats(jankStats: JankStatsUiState, file: File) {
    val jsonObj = buildJsonObject {
      put("totalFrames", jankStats.totalFrames)
      put("jankyFrames", jankStats.jankyFrames)
      put("jankPercentage", jankStats.jankPercentage.value)
      put("avgFrameDurationMs", jankStats.avgFrameDurationMs)

      putJsonArray("stateBreakdown") {
        jankStats.stateBreakdown.forEach { state ->
          add(
            buildJsonObject {
              put("state", state.state)
              put("count", state.count)
            }
          )
        }
      }

      putJsonArray("jankyFramesList") {
        jankStats.jankyFramesList.forEach { frame ->
          add(
            buildJsonObject {
              put("timestampMs", frame.timestampMs)
              put("durationUiMs", frame.durationUiMs)
              put("durationCpuMs", frame.durationCpuMs)
              put("overrunMs", frame.overrunMs)
              put("isJank", frame.isJank)
              putJsonArray("states") {
                frame.states.forEach { (key, value) ->
                  add(
                    buildJsonObject {
                      put("key", key)
                      put("value", value)
                    }
                  )
                }
              }
            }
          )
        }
      }
    }

    file.writeText(json.encodeToString(jsonObj))
  }

  // ============================================================================
  // Plain Text Writers (narrative data)
  // ============================================================================

  /**
   * Writes app exit information to a plain text file.
   * Format: Structured blocks for each exit event with stack traces.
   */
  fun writeAppExits(exits: List<AppExitInfo>, file: File) {
    file.bufferedWriter().use { writer ->
      if (exits.isEmpty()) {
        writer.write("No app exit events recorded.\n")
        return@use
      }

      exits.forEachIndexed { index, exit ->
        if (index > 0) writer.write("\n")
        writer.write("=".repeat(SEPARATOR_WIDTH) + "\n")
        writer.write("[${index + 1}] ${exit.reason.label} (${exit.reason.severity})\n")
        writer.write("=".repeat(SEPARATOR_WIDTH) + "\n")

        writer.write("Timestamp: ${formatFullTimestamp(exit.timestampMs)}\n")
        writer.write("Process: ${exit.processName}\n")
        writer.write("Importance: ${exit.importance.label}\n")
        writer.write("PSS: ${formatBytesFromKb(exit.pssKb)} | RSS: ${formatBytesFromKb(exit.rssKb)}\n")
        exit.description?.let { writer.write("Description: $it\n") }
        writer.write("\nExplanation: ${exit.reason.explanation}\n")

        exit.trace?.let { trace ->
          writer.write("\n--- TRACE ---\n")
          writer.write("$trace\n")
        }
      }
    }
  }

  /**
   * Writes UI hierarchy dump to a plain text file.
   * Format: Raw Radiography output (already formatted as a tree).
   */
  fun writeUiHierarchy(uiHierarchy: String, file: File) {
    file.writeText(uiHierarchy)
  }
}
