package com.opplayer.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppInstrumentationTest {

    @Test
    fun applicationIdIsCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.opplayer.app", context.packageName)
    }

    @Test
    fun appNameResourceIsAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.getString(R.string.app_name).isNotBlank())
    }
}
