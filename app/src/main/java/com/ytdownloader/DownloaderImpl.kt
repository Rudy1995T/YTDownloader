package com.ytdownloader

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Implementation of NewPipe's Downloader interface using OkHttp
 * With YouTube bot detection bypass
 */
class DownloaderImpl private constructor() : Downloader() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    companion object {
        private var instance: DownloaderImpl? = null
        
        // Rotate through several realistic user agents
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1"
        )
        
        fun getInstance(): DownloaderImpl {
            if (instance == null) {
                instance = DownloaderImpl()
            }
            return instance!!
        }
    }
    
    override fun execute(request: Request): Response {
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()
        
        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(
                request.httpMethod(),
                if (dataToSend != null) dataToSend.toRequestBody() else null
            )
        
        // Add headers from request
        for ((key, values) in headers) {
            for (value in values) {
                requestBuilder.addHeader(key, value)
            }
        }
        
        // Add required headers if not present
        if (!headers.containsKey("User-Agent")) {
            requestBuilder.addHeader("User-Agent", USER_AGENTS.random())
        }
        
        // Critical: Add YouTube consent cookie to bypass "Sign in to confirm you're not a bot"
        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            if (!headers.containsKey("Cookie")) {
                // SOCS cookie = consent given, PREF for preferences
                requestBuilder.addHeader("Cookie", "SOCS=CAESEwgDEgk2MjcxMjE1NjQaAmVuIAEaBgiA_t6vBg; PREF=tz=UTC&f6=40000000")
            }
            // Additional headers that help avoid detection
            requestBuilder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            requestBuilder.addHeader("Accept-Language", "en-US,en;q=0.5")
            requestBuilder.addHeader("Sec-Fetch-Dest", "document")
            requestBuilder.addHeader("Sec-Fetch-Mode", "navigate")
            requestBuilder.addHeader("Sec-Fetch-Site", "none")
            requestBuilder.addHeader("Sec-Fetch-User", "?1")
            requestBuilder.addHeader("Upgrade-Insecure-Requests", "1")
        }
        
        val response = client.newCall(requestBuilder.build()).execute()
        
        val responseBody = response.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        
        for (name in response.headers.names()) {
            val values = response.headers.values(name)
            responseHeaders[name] = values.toMutableList()
        }
        
        return Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
    }
}
