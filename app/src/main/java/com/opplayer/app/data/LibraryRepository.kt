package com.opplayer.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "op_player_library"
)

class LibraryRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val librarySerializer = ListSerializer(VideoItem.serializer())
    private val progressSerializer = MapSerializer(String.serializer(), LocalProgress.serializer())
    private val legacyPositionsSerializer = MapSerializer(String.serializer(), Long.serializer())

    val library: Flow<List<VideoItem>> = context.libraryDataStore.data
        .map { prefs -> decodeLibrary(prefs[LIBRARY_KEY]) }
        .flowOn(Dispatchers.IO)

    val localPositions: Flow<Map<String, Long>> = context.libraryDataStore.data
        .map { prefs -> decodeProgress(prefs[LOCAL_POSITIONS_KEY]).mapValues { it.value.positionMs } }
        .flowOn(Dispatchers.IO)

    private fun decodeLibrary(raw: String?): List<VideoItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(librarySerializer, raw) }
            .getOrDefault(emptyList())
    }

    private fun decodeProgress(raw: String?): Map<String, LocalProgress> {
        if (raw.isNullOrBlank()) return emptyMap()

        val current = runCatching { json.decodeFromString(progressSerializer, raw) }.getOrNull()
        if (current != null) return current

        val legacy = runCatching { json.decodeFromString(legacyPositionsSerializer, raw) }
            .getOrDefault(emptyMap())

        return legacy.mapValues { LocalProgress(positionMs = it.value, updatedAt = 0L) }
    }

    suspend fun updateLibrary(transform: (List<VideoItem>) -> List<VideoItem>) {
        context.libraryDataStore.edit { prefs ->
            val current = decodeLibrary(prefs[LIBRARY_KEY])
            prefs[LIBRARY_KEY] = json.encodeToString(librarySerializer, transform(current))
        }
    }

    suspend fun saveLocalPosition(
        uri: String,
        positionMs: Long,
        now: Long = System.currentTimeMillis()
    ) {
        context.libraryDataStore.edit { prefs ->
            val current = decodeProgress(prefs[LOCAL_POSITIONS_KEY]).toMutableMap()

            if (positionMs <= 0L) {
                current.remove(uri)
            } else {
                current[uri] = LocalProgress(positionMs = positionMs, updatedAt = now)
            }

            prefs[LOCAL_POSITIONS_KEY] = json.encodeToString(
                progressSerializer,
                trimProgress(current, MAX_TRACKED_POSITIONS)
            )
        }
    }

    private companion object {
        val LIBRARY_KEY = stringPreferencesKey("library_json_v2")
        val LOCAL_POSITIONS_KEY = stringPreferencesKey("local_positions_json")
        const val MAX_TRACKED_POSITIONS = 300
    }
}
