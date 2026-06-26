package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MRB-TV", appName)
  }

  @Test
  fun `test M3uParser parsing with standard and fallback formats`() {
    val m3uContent = """
      #EXTM3U
      #EXTINF:-1 tvg-logo="https://logo.com/news.png" group-title="News",Global News Channel
      https://stream.com/news.m3u8
      #EXTINF:0 group="Sports",Local Soccer Live
      http://stream.com/sports.m3u8
      #EXTINF:-1,Fallback Channel
      https://stream.com/fallback.m3u8
    """.trimIndent()

    val channels = M3uParser.parse(m3uContent)
    assertNotNull(channels)
    assertEquals(3, channels.size)

    // First channel assertions
    val ch1 = channels[0]
    assertEquals("https://stream.com/news.m3u8", ch1.url)
    assertEquals("Global News Channel", ch1.name)
    assertEquals("https://logo.com/news.png", ch1.logoUrl)
    assertEquals("News", ch1.groupTitle)

    // Second channel assertions
    val ch2 = channels[1]
    assertEquals("http://stream.com/sports.m3u8", ch2.url)
    assertEquals("Local Soccer Live", ch2.name)
    assertEquals("Sports", ch2.groupTitle)

    // Third channel assertions
    val ch3 = channels[2]
    assertEquals("https://stream.com/fallback.m3u8", ch3.url)
    assertEquals("Fallback Channel", ch3.name)
    assertEquals("Uncategorized", ch3.groupTitle)
  }
}

