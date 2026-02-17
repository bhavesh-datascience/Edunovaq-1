package com.example.eduu

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfUtils {
    // Smart function: Reads text but stops if it gets too long
    suspend fun extractTextSmart(context: Context, uri: Uri, pageLimit: Int = 50): String {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""

                // Load PDF
                val document = PDDocument.load(inputStream)

                // Setup Stripper with Limits
                val stripper = PDFTextStripper()
                stripper.startPage = 1
                // Cap the pages to prevent "Payload Too Large" errors
                stripper.endPage = minOf(document.numberOfPages, pageLimit)

                val text = stripper.getText(document)

                document.close()
                inputStream.close()

                return@withContext text.trim()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ""
            }
        }
    }

    // Check if file is small enough (< 3MB) to send directly
    fun isSmallFile(context: Context, uri: Uri): Boolean {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val sizeIndex = cursor?.getColumnIndex(android.provider.OpenableColumns.SIZE)
            cursor?.moveToFirst()
            val size = cursor?.getLong(sizeIndex ?: 0) ?: 0
            cursor?.close()
            // Limit to 3MB for direct raw upload (safest for mobile networks)
            size < (3 * 1024 * 1024)
        } catch (e: Exception) {
            false
        }
    }

    // Read raw bytes for small files
    suspend fun readBytes(context: Context, uri: Uri): ByteArray? {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }
}