package com.ytdownloader

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Implementation of NewPipe's Downloader interface using OkHttp
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
        
        // Add headers
        for ((key, values) in headers) {
            for (value in values) {
                requestBuilder.addHeader(key, value)
            }
        }
        
        // Add User-Agent if not present
        if (!headers.containsKey("User-Agent")) {
            requestBuilder.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
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
