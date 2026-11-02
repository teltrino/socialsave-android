package com.sahab.downloader

import org.junit.Assert.*
import org.junit.Test

class LinkAndNetworkTest {
    @Test fun sharedAndCanonicalYoutubeLinksHaveTheSamePreviewKey() {
        val expected = "ODDRRXMi22E"
        listOf(
            "https://youtu.be/ODDRRXMi22E?si=share-token",
            "https://www.youtube.com/watch?v=ODDRRXMi22E&list=playlist",
            "https://m.youtube.com/watch?feature=share&v=ODDRRXMi22E",
            "https://youtube.com/shorts/ODDRRXMi22E",
            "https://www.youtube.com/live/ODDRRXMi22E?feature=share"
        ).forEach { assertEquals(expected, YouTubeLink.videoId(it)) }
    }

    @Test fun nonYoutubeHostsAndInvalidIdsDoNotUseYoutubePreview() {
        assertNull(YouTubeLink.videoId("https://youtube.com.example.org/watch?v=ODDRRXMi22E"))
        assertNull(YouTubeLink.videoId("https://www.youtube.com/watch?v=invalid"))
        assertNull(YouTubeLink.videoId("https://vimeo.com/22439234"))
    }

    @Test fun reportedDnsFailureTriggersRecoveryAndReadableSummary() {
        val details = "WARNING: Signature solving failed\nERROR:\n[download] Got error: [Errno 7] No address associated with hostname. Giving up after 3 retries\nERROR:"
        assertTrue(NetworkFailure.canRetry(details))
        assertTrue(DownloadFailure.summary(details).contains("DNS lookup failed"))
        assertFalse(DownloadFailure.summary(details).endsWith("ERROR:"))
    }

    @Test fun playerTimeoutAndExpiredUrlCanBeRetried() {
        assertTrue(NetworkFailure.canRetry("Failed to load player: The read operation timed out"))
        assertTrue(NetworkFailure.canRetry("ERROR: HTTP Error 403: Forbidden"))
    }

    @Test fun permanentErrorsDoNotTriggerNetworkRetry() {
        assertFalse(NetworkFailure.canRetry("ERROR: This video is private"))
        assertFalse(NetworkFailure.canRetry("ERROR: No space left on device"))
        assertFalse(NetworkFailure.canRetry("ERROR: Unsupported URL"))
    }
}
