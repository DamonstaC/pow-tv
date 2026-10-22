package tv.own.owntv.core.stalker

import org.junit.Assert.*
import org.junit.Test

class PowStalkerCompatibilityTest {
    @Test fun artworkUsesHostRootAndPreservesExternalImages() {
        val api = "https://portal.example/stalker_portal/server/load.php"
        assertEquals("https://portal.example/images/poster.jpg", StalkerClient.resolveArtwork(api, "/images/poster.jpg"))
        assertEquals("https://portal.example/images/poster.jpg", StalkerClient.resolveArtwork(api, "images/poster.jpg"))
        assertEquals("https://cdn.example/a.jpg", StalkerClient.resolveArtwork(api, "https://cdn.example/a.jpg"))
        assertEquals("https://cdn.example/a.jpg", StalkerClient.resolveArtwork(api, "//cdn.example/a.jpg"))
        assertNull(StalkerClient.resolveArtwork(api, "0"))
        assertNull(StalkerClient.resolveArtwork(api, "javascript:bad"))
    }

    @Test fun onlyFileBackedMoviesUseFileLookup() {
        val row = mapOf("id" to "42", "has_files" to "1", "is_series" to "0", "cmd" to "/media/42.mpg")
        assertEquals("powtv-vod-file:42", StalkerClient.movieCommand(row))
        assertEquals("/media/42.mpg", StalkerClient.movieCommand(row + ("is_series" to "1")))
        assertEquals("/media/42.mpg", StalkerClient.movieCommand(row + ("has_files" to "0")))
        assertEquals("/media/42.mpg", StalkerClient.movieCommand(row + ("is_file" to "1")))
    }
}
