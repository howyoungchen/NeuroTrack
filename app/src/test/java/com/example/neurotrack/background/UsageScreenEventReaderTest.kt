package com.example.neurotrack.background

import android.app.usage.UsageEvents
import com.example.neurotrack.data.SCREEN_OFF
import com.example.neurotrack.data.SCREEN_ON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageScreenEventReaderTest {
    @Test
    fun screenEventTypeForUsageEventType_mapsScreenInteractiveToScreenOn() {
        assertEquals(
            SCREEN_ON,
            UsageScreenEventReader.screenEventTypeForUsageEventType(
                UsageEvents.Event.SCREEN_INTERACTIVE,
            ),
        )
    }

    @Test
    fun screenEventTypeForUsageEventType_mapsScreenNonInteractiveToScreenOff() {
        assertEquals(
            SCREEN_OFF,
            UsageScreenEventReader.screenEventTypeForUsageEventType(
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            ),
        )
    }

    @Test
    fun screenEventTypeForUsageEventType_ignoresUnrelatedUsageEvents() {
        assertNull(
            UsageScreenEventReader.screenEventTypeForUsageEventType(
                UsageEvents.Event.ACTIVITY_RESUMED,
            ),
        )
    }
}
