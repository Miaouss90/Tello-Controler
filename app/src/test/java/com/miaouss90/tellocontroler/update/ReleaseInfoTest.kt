package com.miaouss90.tellocontroler.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseInfoTest {
    @Test
    fun `picks the apk asset`() {
        val json = """
            {"tag_name":"v0.3.42","assets":[
              {"name":"notes.txt","browser_download_url":"https://x/notes.txt","size":10},
              {"name":"Tello-Controler-v0.3.42.apk","browser_download_url":"https://x/app.apk","size":1234}
            ]}
        """.trimIndent()
        assertEquals(ReleaseInfo("v0.3.42", "https://x/app.apk", 1234), ReleaseInfo.fromGitHubJson(json))
    }

    @Test
    fun `release without apk is ignored`() {
        assertNull(ReleaseInfo.fromGitHubJson("""{"tag_name":"v1","assets":[]}"""))
    }
}
