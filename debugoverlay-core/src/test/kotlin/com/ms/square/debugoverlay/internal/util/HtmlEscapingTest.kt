package com.ms.square.debugoverlay.internal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HtmlEscapingTest {

  @Test
  fun `escapeHtml returns same instance when no special characters`() {
    val input = "Hello DebugOverlay 123"
    assertSame(input, input.escapeHtml())
  }

  @Test
  fun `escapeHtml escapes all special characters`() {
    val input = """<&>"'"""
    val expected = "&lt;&amp;&gt;&quot;&#39;"
    assertEquals(expected, input.escapeHtml())
  }

  @Test
  fun `escapeHtml escapes typical XSS payload`() {
    val input = "<script>alert('xss')</script>"
    val expected = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
    assertEquals(expected, input.escapeHtml())
  }
}
