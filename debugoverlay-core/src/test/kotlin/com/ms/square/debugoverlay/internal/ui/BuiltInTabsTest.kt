package com.ms.square.debugoverlay.internal.ui

import com.google.common.truth.Truth.assertThat
import com.ms.square.debugoverlay.DebugTab
import org.junit.Test

class BuiltInTabsTest {

  @Test
  fun `resolveVisibleTabs returns configTabs unchanged when no custom log source`() {
    val tabs = listOf(DebugTab.Logcat, DebugTab.Network)
    val result = resolveVisibleTabs(tabs, hasCustomLogSource = false)
    assertThat(result).isEqualTo(tabs)
  }

  @Test
  fun `resolveVisibleTabs injects CustomLog after Logcat when custom log source exists`() {
    val tabs = listOf(DebugTab.Logcat, DebugTab.Network, DebugTab.DeviceInfo)
    val result = resolveVisibleTabs(tabs, hasCustomLogSource = true)

    assertThat(result).hasSize(4)
    assertThat(result[0]).isSameInstanceAs(DebugTab.Logcat)
    assertThat(result[1]).isSameInstanceAs(CustomLog)
    assertThat(result[2]).isSameInstanceAs(DebugTab.Network)
    assertThat(result[3]).isSameInstanceAs(DebugTab.DeviceInfo)
  }

  @Test
  fun `resolveVisibleTabs injects CustomLog at first position when Logcat is absent`() {
    val tabs = listOf(DebugTab.Network, DebugTab.DeviceInfo)
    val result = resolveVisibleTabs(tabs, hasCustomLogSource = true)

    assertThat(result).hasSize(3)
    assertThat(result[0]).isSameInstanceAs(CustomLog)
    assertThat(result[1]).isSameInstanceAs(DebugTab.Network)
    assertThat(result[2]).isSameInstanceAs(DebugTab.DeviceInfo)
  }

  @Test
  fun `resolveVisibleTabs with defaults injects CustomLog after Logcat`() {
    val result = resolveVisibleTabs(DebugTab.defaults, hasCustomLogSource = true)

    assertThat(result).hasSize(DebugTab.defaults.size + 1)
    assertThat(result[0]).isSameInstanceAs(DebugTab.Logcat)
    assertThat(result[1]).isSameInstanceAs(CustomLog)
    assertThat(result[2]).isSameInstanceAs(DebugTab.Network)
  }

  @Test
  fun `resolveVisibleTabs preserves custom tabs in list`() {
    val customTab = DebugTab(title = "Flags") {}
    val tabs = listOf(DebugTab.Logcat, customTab, DebugTab.Network)
    val result = resolveVisibleTabs(tabs, hasCustomLogSource = false)

    assertThat(result).hasSize(3)
    assertThat(result[1]).isSameInstanceAs(customTab)
  }

  @Test
  fun `resolveVisibleTabs handles empty list`() {
    val result = resolveVisibleTabs(emptyList(), hasCustomLogSource = false)
    assertThat(result).isEmpty()
  }

  @Test
  fun `resolveVisibleTabs injects CustomLog into empty list when custom log source exists`() {
    val result = resolveVisibleTabs(emptyList(), hasCustomLogSource = true)

    assertThat(result).hasSize(1)
    assertThat(result[0]).isSameInstanceAs(CustomLog)
  }
}
