package com.opendedownloader.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class DownloadEngineTest {

    @Test
    fun testSha256Calculation() {
        // Create a temporary file with known content
        val tempFile = File.createTempFile("test_sha256", ".txt")
        tempFile.deleteOnExit()

        val content = "Hello OpenDownloader Android TV App!"
        FileOutputStream(tempFile).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
        }

        // Calculate expected SHA-256 manually
        val digest = MessageDigest.getInstance("SHA-256")
        val expectedHashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        val expectedHash = expectedHashBytes.joinToString("") { "%02x".format(it) }

        // Calculate actual SHA-256 using helper
        val actualHash = calculateFileSha256(tempFile)

        assertEquals(expectedHash, actualHash)
    }

    private fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        val fis = java.io.FileInputStream(file)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        fis.close()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
