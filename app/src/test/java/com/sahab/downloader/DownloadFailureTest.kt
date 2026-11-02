package com.sahab.downloader

import org.junit.Assert.*
import org.junit.Test

class DownloadFailureTest {
    @Test fun finalErrorSurvivesLongWarningPreamble() {
        val warnings = "WARNING: old engine and skipped formats\n".repeat(1200)
        val error = "ERROR: [youtube] test-id: Requested format is not available"
        val details = DownloadFailure.details(IllegalStateException(warnings + error))
        assertTrue(details.endsWith(error))
        assertEquals("[youtube] test-id: Requested format is not available", DownloadFailure.summary(details))
        assertTrue(details.length <= 24000)
    }

    @Test fun readableErrorsDoNotContainTerminalColorCodes() {
        val details = DownloadFailure.details(Exception("\u001B[31mERROR: HTTP Error 403: Forbidden\u001B[0m"))
        assertFalse(details.contains('\u001B'))
        assertEquals("HTTP Error 403: Forbidden", DownloadFailure.summary(details))
    }

    @Test fun nonExtractorFailuresStillHaveAUsefulMessage() {
        val details = DownloadFailure.details(java.io.IOException("No space left on device"))
        assertEquals("No space left on device", DownloadFailure.summary(details))
    }
}
