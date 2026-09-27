package com.miaouss90.tellocontroler.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    @Test
    fun `higher build number is newer`() {
        assertTrue(AppVersion.isNewer("v0.3.41", "0.3.40"))
        assertTrue(AppVersion.isNewer("v0.3.100", "0.3.99"))
    }

    @Test
    fun `same or older is not newer`() {
        assertFalse(AppVersion.isNewer("v0.3.40", "0.3.40"))
        assertFalse(AppVersion.isNewer("v0.2.0", "0.3.1"))
    }

    @Test
    fun `local dev build is older than any CI build of the same line`() {
        assertTrue(AppVersion.isNewer("v0.3.33", "0.3.0-dev"))
    }

    @Test
    fun `missing segments count as zero`() {
        assertFalse(AppVersion.isNewer("v1.0", "1.0.0"))
        assertTrue(AppVersion.isNewer("v1.0.1", "1.0"))
    }

    @Test
    fun `uppercase V prefix is accepted`() {
        assertTrue(AppVersion.isNewer("V0.4.1", "0.4.0"))
    }
}
