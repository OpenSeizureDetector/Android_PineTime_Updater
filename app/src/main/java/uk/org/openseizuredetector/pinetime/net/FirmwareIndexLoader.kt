package uk.org.openseizuredetector.pinetime.net

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class FirmwareItem(
    val name: String,
    val url: String,
    val version: String? = null,
    val notes: String? = null
)

object FirmwareIndexLoader {
    private val client = OkHttpClient()

    fun load(url: String): List<FirmwareItem> {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()

            return parseIndex(body)
        }
    }

    private fun parseIndex(json: String): List<FirmwareItem> {
        try {
            val root = JSONObject(json)
            if (root.has("firmwares")) {
                val arr = root.getJSONArray("firmwares")
                return parseArray(arr)
            }
            val items = mutableListOf<FirmwareItem>()
            val names = root.keys()
            while (names.hasNext()) {
                val key = names.next()
                val value = root.optString(key, null)
                if (value != null && value.startsWith("http") && value.endsWith(".zip")) {
                    items += FirmwareItem(name = key, url = value)
                }
            }
            if (items.isNotEmpty()) return items
        } catch (_: Throwable) { }
        return try {
            val arr = JSONArray(json)
            parseArray(arr)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseArray(arr: JSONArray): List<FirmwareItem> {
        val items = mutableListOf<FirmwareItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "Firmware ${i + 1}")
            val url = obj.optString("url", "")
            if (url.isBlank()) continue
            val version = obj.optString("version", null)
            val notes = obj.optString("notes", null)
            items += FirmwareItem(name = name, url = url, version = version, notes = notes)
        }
        return items
    }
}
