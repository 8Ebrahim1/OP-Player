package com.opplayer.app.data

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A pattern is handed to the player activity as a parcel and restored after process death, so
 * the custom [android.os.Parcelable] handling has to survive a round trip and must repair an
 * old or corrupt parcel instead of throwing from `init`.
 */
@RunWith(AndroidJUnit4::class)
class EpisodePatternParcelTest {

    @Test
    fun aRoundTripKeepsEveryField() {
        val pattern = EpisodePattern(
            prefix = "https://cdn.test/show-",
            suffix = ".mkv",
            episode = 7,
            pad = 3,
            step = 2
        )

        val parcel = Parcel.obtain()
        val restored = try {
            parcel.writeParcelable(pattern, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            parcel.readParcelable<EpisodePattern>(EpisodePattern::class.java.classLoader)
        } finally {
            parcel.recycle()
        }

        assertEquals(pattern, restored)
        assertEquals("https://cdn.test/show-007.mkv", restored?.url)
    }

    @Test
    fun aLegacyParcelIsRepairedInsteadOfCrashing() {
        val parcel = Parcel.obtain()
        val restored = try {
            parcel.writeString("https://cdn.test/show-")
            parcel.writeString(".mkv")
            parcel.writeInt(-5)
            parcel.writeInt(0)
            parcel.writeInt(0)
            parcel.setDataPosition(0)

            EpisodePattern.create(parcel)
        } finally {
            parcel.recycle()
        }

        assertEquals(0, restored.episode)
        assertEquals(1, restored.pad)
        assertEquals(1, restored.step)
    }
}
