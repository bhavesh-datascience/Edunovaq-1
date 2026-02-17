package com.example.eduu

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object YouTubeUtils {

    data class VideoInfo(
        val title: String,
        val description: String,
        val channel: String
    )

    // Extract Video ID from various URL formats (youtu.be, youtube.com)
    fun extractVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group() else null
    }

    // Fetch Metadata using Jsoup (Smart Scraping)
    suspend fun getVideoDetails(url: String): VideoInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // Connect to the URL
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()

                // Extract Meta Tags
                val title = doc.select("meta[property=og:title]").attr("content")
                val desc = doc.select("meta[property=og:description]").attr("content")
                val channel = doc.select("link[itemprop=name]").attr("content")

                if (title.isNotEmpty()) {
                    VideoInfo(title, desc, channel)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}