package dev.xboyrex.darkplayer.feature.player.service

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import dev.xboyrex.darkplayer.feature.player.utils.DisplaySyncEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SubtitleDeduplicationTest {

    @Test
    fun `deduplicateSubtitleConfigurations preserves unique tracks when enriched twice`() {
        val subEn = MediaItem.SubtitleConfiguration.Builder(Uri.parse("file:///Movies/sub.en.srt"))
            .setId("sub-en")
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("en")
            .build()
        val subHi = MediaItem.SubtitleConfiguration.Builder(Uri.parse("file:///Movies/sub.hi.srt"))
            .setId("sub-hi")
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("hi")
            .build()

        val firstPass = deduplicateSubtitleConfigurations(
            existing = listOf(subEn),
            added = listOf(subEn, subHi),
        )
        val secondPass = deduplicateSubtitleConfigurations(
            existing = firstPass,
            added = listOf(subEn, subHi),
        )

        assertEquals(2, firstPass.size)
        assertEquals(listOf("sub-en", "sub-hi"), secondPass.map { it.id })
    }

    @Test
    fun `DisplaySyncEngine matches cadence multiples for 24, 25, 30, 50 and 60 fps`() {
        assertTrue(DisplaySyncEngine.isCadenceMultiple(120f, 24f))
        assertTrue(DisplaySyncEngine.isCadenceMultiple(120f, 23.976f))
        assertTrue(DisplaySyncEngine.isCadenceMultiple(120f, 30f))
        assertTrue(DisplaySyncEngine.isCadenceMultiple(120f, 60f))
        assertTrue(DisplaySyncEngine.isCadenceMultiple(100f, 25f))
        assertTrue(DisplaySyncEngine.isCadenceMultiple(50f, 25f))
        assertFalse(DisplaySyncEngine.isCadenceMultiple(60f, 24f))
        assertFalse(DisplaySyncEngine.isCadenceMultiple(60f, 25f))
    }
}
