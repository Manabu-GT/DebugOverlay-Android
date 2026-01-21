package com.ms.square.debugoverlay.internal.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HtmlEscapingTest {

  @Test
  fun `escapeHtml returns same instance when no special characters`() {
    val input = "Hello DebugOverlay 123"
    assertThat(input.escapeHtml()).isSameInstanceAs(input)
  }

  @Test
  fun `escapeHtml escapes all special characters`() {
    val input = """<&>"'"""
    val expected = "&lt;&amp;&gt;&quot;&#39;"
    assertThat(input.escapeHtml()).isEqualTo(expected)
  }

  @Test
  fun `escapeHtml escapes typical XSS payload`() {
    val input = "<script>alert('xss')</script>"
    val expected = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
    assertThat(input.escapeHtml()).isEqualTo(expected)
  }
}
