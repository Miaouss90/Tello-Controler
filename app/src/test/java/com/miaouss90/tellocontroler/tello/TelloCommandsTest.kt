package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class TelloCommandsTest {
    @Test
    fun `rc values are clamped to SDK range`() {
        assertEquals("rc -100 100 0 42", TelloCommands.rc(-150, 300, 0, 42))
    }
}
