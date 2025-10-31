package com.ms.square.debugoverlay.sample.data.source

import android.util.Xml
import com.ms.square.debugoverlay.sample.data.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

private const val RSS_URL = "https://androidweekly.net/rss.xml"
private val NAMESPACE = null

/**
 * Data source for fetching Android Weekly RSS feed.
 */
class AndroidWeeklyDataSource(private val httpClient: OkHttpClient = OkHttpClient()) {
  /**
   * Fetches the list of Android Weekly issues from the RSS feed.
   *
   * @return List of issues, or empty list if fetch/parse fails
   */
  suspend fun fetchFeedItems(): Result<List<FeedItem>> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(RSS_URL)
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          Timber.e("HTTP request failed with code: ${response.code}")
          return@withContext Result.failure(
            IOException("HTTP error: ${response.code}")
          )
        }

        response.body.byteStream().use { inputStream ->
          val issues = parseRssFeed(inputStream)
          Result.success(issues)
        }
      }
    } catch (e: IOException) {
      Timber.e(e, "Network error fetching Android Weekly RSS")
      Result.failure(e)
    } catch (e: XmlPullParserException) {
      Timber.e(e, "XML parsing error")
      Result.failure(e)
    }
  }

  /**
   * Parses the RSS feed XML and extracts [FeedItem] information.
   */
  private fun parseRssFeed(inputStream: InputStream): List<FeedItem> {
    val parser = Xml.newPullParser().apply {
      setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
      setInput(inputStream, null)
    }

    val issues = mutableListOf<FeedItem>()
    parser.nextTag()

    // Find the <channel> element
    parser.require(XmlPullParser.START_TAG, NAMESPACE, "rss")
    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.eventType != XmlPullParser.START_TAG) {
        continue
      }
      if (parser.name == "channel") {
        issues.addAll(readChannel(parser))
      } else {
        skip(parser)
      }
    }

    return issues
  }

  /**
   * Reads the <channel> element and extracts all <item> elements.
   */
  private fun readChannel(parser: XmlPullParser): List<FeedItem> {
    val issues = mutableListOf<FeedItem>()
    parser.require(XmlPullParser.START_TAG, NAMESPACE, "channel")

    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.eventType != XmlPullParser.START_TAG) {
        continue
      }
      if (parser.name == "item") {
        issues.add(readItem(parser))
      } else {
        skip(parser)
      }
    }

    return issues
  }

  /**
   * Reads a single <item> element and converts it to a [FeedItem].
   */
  private fun readItem(parser: XmlPullParser): FeedItem {
    parser.require(XmlPullParser.START_TAG, NAMESPACE, "item")

    var title = ""
    var link = ""
    var pubDate = ""
    var description = ""

    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.eventType != XmlPullParser.START_TAG) {
        continue
      }

      when (parser.name) {
        "title" -> title = readText(parser, "title")
        "link" -> link = readText(parser, "link").replace("rss.xml", "")
        "pubDate" -> pubDate = readText(parser, "pubDate").replace(" +0000", "")
        "description" -> description = readText(parser, "description")
        else -> skip(parser)
      }
    }

    // Extract issue number from title (e.g., "Issue #600" -> 600)
    val issueNumber = extractIssueNumber(title)

    return FeedItem(
      id = issueNumber,
      title = title,
      url = link,
      publishDate = pubDate,
      description = description // Keep HTML for proper rendering
    )
  }

  /**
   * Reads the text content of an XML element.
   */
  private fun readText(parser: XmlPullParser, tagName: String): String {
    parser.require(XmlPullParser.START_TAG, NAMESPACE, tagName)
    val text = if (parser.next() == XmlPullParser.TEXT) {
      parser.text
    } else {
      ""
    }
    parser.nextTag()
    parser.require(XmlPullParser.END_TAG, NAMESPACE, tagName)
    return text
  }

  /**
   * Skips the current XML element and all its children.
   */
  private fun skip(parser: XmlPullParser) {
    if (parser.eventType != XmlPullParser.START_TAG) {
      error("skip - eventType(${parser.eventType}) != START_TAG")
    }
    var depth = 1
    while (depth != 0) {
      when (parser.next()) {
        XmlPullParser.END_TAG -> depth--
        XmlPullParser.START_TAG -> depth++
      }
    }
  }

  /**
   * Extracts the issue number from the title.
   * Example: "Issue #600" -> 600
   */
  private fun extractIssueNumber(title: String): Int {
    val regex = """Issue\s+#?(\d+)""".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = regex.find(title)
    return matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
  }
}
