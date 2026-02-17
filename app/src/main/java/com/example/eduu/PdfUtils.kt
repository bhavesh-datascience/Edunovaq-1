package com.example.eduu

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractTextFromPdf(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null

            // Load Document
            val document = PDDocument.load(inputStream)

            // Extract Text
            val stripper = PDFTextStripper()
            // Optional: stripper.sortByPosition = true // Better for multi-column PDFs
            val text = stripper.getText(document)

            document.close()
            inputStream.close()

            return@withContext text
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}