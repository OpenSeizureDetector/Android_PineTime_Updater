package uk.org.openseizuredetector.pinetime.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

object FirmwareDownloader {
    private val client = OkHttpClient()

    fun downloadWithProgress(
        url: String,
        outFile: File,
        progress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Boolean {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return false
            val body = resp.body ?: return false
            val totalLength = body.contentLength()
            outFile.outputStream().use { fos ->
                body.source().use { source ->
                    fos.sink().buffer().use { sink ->
                        var totalRead = 0L
                        val bufSize = 8 * 1024L
                        val buffer = okio.Buffer()
                        while (true) {
                            val read = source.read(buffer, bufSize)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            totalRead += read
                            progress(totalRead, totalLength)
                        }
                        sink.flush()
                    }
                }
            }
            return true
        }
    }
}
