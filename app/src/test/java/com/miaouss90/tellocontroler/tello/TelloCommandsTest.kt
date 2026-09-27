package com.miaouss90.tellocontroler.tello

import org.junit.Assert.assertEquals
import org.junit.Test

class TelloCommandsTest {
    @Test
    fun `rc values are clamped to SDK range`() {
        assertEquals("rc -100 100 0 42", TelloCommands.rc(-150, 300, 0, 42))
    }

    @Test
    fun `ok response is Ok, anything else is Error`() {
        assertEquals(CommandResult.Ok, TelloCommands.parseResponse(" OK\r\n"))
        assertEquals(CommandResult.Error("error Motor stop"), TelloCommands.parseResponse("error Motor stop"))
    }
}
