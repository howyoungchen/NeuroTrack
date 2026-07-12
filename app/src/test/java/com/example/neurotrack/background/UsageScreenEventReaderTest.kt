package com.example.neurotrack.background

import android.app.usage.UsageEvents
import com.example.neurotrack.domain.DeviceInteractionType
import com.example.neurotrack.domain.ScreenEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageScreenEventReaderTest {
    @Test
    fun screenEventTypeForUsageEventType_mapsScreenInteractiveToScreenOn() {
        assertEquals(
            ScreenEventType.SCREEN_ON,
            UsageScreenEventReader.screenEventTypeForUsageEventType(
                UsageEvents.Event.SCREEN_INTERACTIVE,
            ),
        )
    }

    @Test
    fun screenEventTypeForUsageEventType_mapsScreenNonInteractiveToScreenOff() {
        assertEquals(
            ScreenEventType.SCREEN_OFF,
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

    @Test
    fun interactionTypeForUsageEventType_mapsKeyguardHiddenToUnlocked() {
        assertEquals(
            DeviceInteractionType.KEYGUARD_UNLOCKED,
            UsageScreenEventReader.interactionTypeForUsageEventType(
                UsageEvents.Event.KEYGUARD_HIDDEN,
            ),
        )
    }

    @Test
    fun interactionTypeForUsageEventType_mapsActivityResumedToForegroundApp() {
        assertEquals(
            DeviceInteractionType.FOREGROUND_APP,
            UsageScreenEventReader.interactionTypeForUsageEventType(
                UsageEvents.Event.ACTIVITY_RESUMED,
            ),
        )
    }

}
