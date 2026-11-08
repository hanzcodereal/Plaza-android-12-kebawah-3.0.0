package com.decio.plaza

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Riwayat tontonan di HP ini. Setiap video yang dibuka langsung masuk (terbaru di depan), jadi
 * Riwayat di Perpustakaan selalu bertambah walau YouTube terlambat mengirim riwayat akun.
 * Perpustakaan menggabungkan ini dengan riwayat akun.
 */
object LocalHistory {
    private const val MAX = 100
    private var prefs: SharedPreferences? = null

    var items by mutableStateOf<List<VideoItem>>(emptyList())
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("local_history", Context.MODE_PRIVATE)
        prefs = p
        val raw = p.getString("items", null) ?: return
        items = runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let(::fromJson) }
        }.getOrDefault(emptyList())
    }

    fun add(video: VideoItem) {
        if (video.id.isBlank()) return
        items = (listOf(video.copy(watched = null)) + items.filter { it.id != video.id }).take(MAX)
        val a = JSONArray()
        items.forEach { a.put(toJson(it)) }
        prefs?.edit()?.putString("items", a.toString())?.apply()
    }

    fun clear() {
        items = emptyList()
        prefs?.edit()?.remove("items")?.apply()
    }

    private fun toJson(v: VideoItem) = JSONObject()
        .put("id", v.id).put("title", v.title).put("channel", v.channel)
        .put("channelUrl", v.channelUrl ?: JSONObject.NULL).put("thumbnail", v.thumbnail ?: JSONObject.NULL)
        .put("avatar", v.avatar ?: JSONObject.NULL).put("durationSec", v.durationSec).put("views", v.views)
        .put("uploaded", v.uploaded ?: JSONObject.NULL).put("live", v.live)

    private fun fromJson(o: JSONObject): VideoItem? {
        val id = o.optString("id").ifBlank { return null }
        fun str(k: String) = if (o.isNull(k)) null else o.optString(k).ifBlank { null }
        return VideoItem(
            id = id, title = o.optString("title"), channel = o.optString("channel"),
            channelUrl = str("channelUrl"), thumbnail = str("thumbnail"), avatar = str("avatar"),
            durationSec = o.optLong("durationSec"), views = o.optLong("views", -1), uploaded = str("uploaded"),
            live = o.optBoolean("live"),
        )
    }
}
