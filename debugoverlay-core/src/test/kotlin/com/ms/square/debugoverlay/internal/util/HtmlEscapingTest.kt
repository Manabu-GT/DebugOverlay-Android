package com.ms.square.debugoverlay.internal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HtmlEscapingTest {

  @Test
  fun escapeHtml_noSpecialCharacters_returnsSameInstance() {
    val input = "Hello DebugOverlay 123"
    assertSame(input, input.escapeHtml())
  }

  @Test
  fun escapeHtml_escapesAllSpecialCharacters() {
    val input = """<&>"'"""
    val expected = "&lt;&amp;&gt;&quot;&#39;"
    assertEquals(expected, input.escapeHtml())
  }

  @Test
  fun escapeHtml_escapesTypicalXssPayload() {
    val input = "<script>alert('xss')</script>"
    val expected = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"
    assertEquals(expected, input.escapeHtml())
  }
}
