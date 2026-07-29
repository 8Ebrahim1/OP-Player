package com.opplayer.app.ui

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Application.opPlayerStore by preferencesDataStore(name = "op_player_library")
private val libraryKey = stringPreferencesKey("library_json")

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    init {
        viewModelScope.launch {
            application.opPlayerStore.data.collectLatest { preferences ->
                _videos.value = decode(preferences[libraryKey])
            }
        }
    }

    fun addVideo(title: String, url: String, subtitleUrl: String = "") = update {
        listOf(VideoItem(System.currentTimeMillis(), title, url, subtitleUrl)) + it
    }
    fun toggleFavorite(id: Long) = update { videos -> videos.map { if (it.id == id) it.copy(favorite = !it.favorite) else it } }
    fun removeVideo(id: Long) = update { videos -> videos.filterNot { it.id == id } }
    fun savePosition(id: Long, positionMs: Long) = update { videos ->
        videos.map { if (it.id == id) it.copy(positionMs = positionMs, lastPlayedAt = System.currentTimeMillis()) else it }
    }

    private fun update(transform: (List<VideoItem>) -> List<VideoItem>) {
        val result = transform(_videos.value)
        _videos.value = result
        viewModelScope.launch { getApplication<Application>().opPlayerStore.edit { it[libraryKey] = encode(result) } }
    }

    private fun encode(videos: List<VideoItem>) = JSONArray().apply {
        videos.forEach { video -> put(JSONObject().apply {
            put("id", video.id); put("title", video.title); put("url", video.url)
            put("subtitleUrl", video.subtitleUrl); put("positionMs", video.positionMs)
            put("favorite", video.favorite); put("lastPlayedAt", video.lastPlayedAt)
        }) }
    }.toString()

    private fun decode(raw: String?): List<VideoItem> = try {
        val array = JSONArray(raw ?: "[]")
        List(array.length()) { index -> array.getJSONObject(index).let { json ->
            VideoItem(json.optLong("id"), json.optString("title"), json.optString("url"), json.optString("subtitleUrl"), json.optLong("positionMs"), json.optBoolean("favorite"), json.optLong("lastPlayedAt"))
        } }
    } catch (_: Exception) { emptyList() }
}
