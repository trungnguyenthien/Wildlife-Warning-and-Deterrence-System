package com.wildlife.deterrence.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException

class SseClient(private val okHttpClient: OkHttpClient) {
    private var job: Job? = null
    private var isRunning = false
    private var activeCall: okhttp3.Call? = null

    fun startListening(
        url: String,
        token: String,
        onEventReceived: (event: String, data: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (isRunning) return
        isRunning = true
        job = CoroutineScope(Dispatchers.IO).launch {
            var retryDelay = 2000L
            while (isRunning) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("Authorization", token)
                        .header("Accept", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .build()

                    val call = okHttpClient.newCall(request)
                    activeCall = call

                    call.execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("SSE Connection failed: $response")
                        }
                        retryDelay = 2000L // Reset delay
                        val reader = BufferedReader(response.body?.charStream() ?: throw IOException("Empty response body"))
                        var line: String? = null
                        var currentEvent = ""
                        var currentData = ""

                        while (isRunning && reader.readLine().also { line = it } != null) {
                            val l = line?.trim() ?: ""
                            if (l.isEmpty()) {
                                if (currentEvent.isNotEmpty() || currentData.isNotEmpty()) {
                                    onEventReceived(currentEvent, currentData)
                                    currentEvent = ""
                                    currentData = ""
                                }
                            } else if (l.startsWith("event:")) {
                                currentEvent = l.substring(6).trim()
                            } else if (l.startsWith("data:")) {
                                currentData = l.substring(5).trim()
                            }
                        }

                        // Nếu kết nối kết thúc bình thường (EOF) khi vẫn đang chạy, ném lỗi để đi vào nhánh retry delay
                        if (isRunning) {
                            throw IOException("Connection closed by server (EOF)")
                        }
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        onError(e)
                        delay(retryDelay)
                        retryDelay = (retryDelay * 2).coerceAtMost(30000L) // Backoff
                    }
                } finally {
                    activeCall = null
                }
            }
        }
    }

    fun stopListening() {
        isRunning = false
        activeCall?.cancel()
        activeCall = null
        job?.cancel()
        job = null
    }
}
