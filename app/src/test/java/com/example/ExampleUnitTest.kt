package com.example

import com.example.data.repository.DownloadManagerHelper
import com.example.data.repository.SeedData
import com.example.ui.player.formatTimeMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testSeedDataIntegrity() {
    assertTrue(SeedData.sampleMovies.isNotEmpty())
    assertTrue(SeedData.sampleSeries.isNotEmpty())

    SeedData.sampleMovies.forEach { movie ->
      assertFalse(movie.isSeries)
      assertTrue(movie.streamUrl.isNotBlank())
      assertTrue(movie.rating > 0.0)
    }

    SeedData.sampleSeries.forEach { series ->
      assertTrue(series.isSeries)
      assertTrue(series.episodes.isNotEmpty())
      series.episodes.forEach { ep ->
        assertTrue(ep.streamUrl.isNotBlank())
        assertEquals(series.id, ep.seriesId)
      }
    }
  }

  @Test
  fun testTimeFormatting() {
    assertEquals("00:00", formatTimeMs(0))
    assertEquals("01:05", formatTimeMs(65000))
    assertEquals("1:01:05", formatTimeMs(3665000))
  }
}
