package com.opplayer.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opplayer.app.player.PlaybackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "op_player_library"
)

class LibraryRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.libraryDataStore)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val librarySerializer = ListSerializer(VideoItem.serializer())
    private val libraryProgressSerializer =
        MapSerializer(String.serializer(), LibraryProgress.serializer())
    private val progressSerializer = MapSerializer(String.serializer(), LocalProgress.serializer())
    private val legacyPositionsSerializer = MapSerializer(String.serializer(), Long.serializer())

    val library: Flow<List<VideoItem>> = dataStore.data
        .map { prefs ->
            val items = readLibrary(prefs).valueOr(emptyList()) ?: return@map emptyList()
            val progress = readLibraryProgress(prefs).valueOr(emptyMap()).orEmpty()

            items.map { item -> item.withProgress(progress[item.id]) }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    val localPositions: Flow<Map<String, Long>> = dataStore.data
        .map { prefs ->
            readLocalProgress(prefs)
                .valueOr(emptyMap())
                .orEmpty()
                .mapValues { it.value.positionMs }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    suspend fun updateLibrary(transform: (List<VideoItem>) -> List<VideoItem>): Boolean {
        var written = false

        dataStore.edit { prefs ->
            val stored = readLibrary(prefs)
            val current = stored.valueOr(emptyList())

            if (current == null) {
                backupCorrupt(prefs, LIBRARY_KEY, LIBRARY_BACKUP_KEY, stored)
                return@edit
            }

            val updated = transform(current).map { it.withoutProgress() }
            prefs[LIBRARY_KEY] = json.encodeToString(librarySerializer, updated)
            written = true
        }

        return written
    }

    suspend fun migrateLegacyDataIfNeeded(): Boolean {
        var migrated = false

        dataStore.edit { prefs ->
            val currentVersion = prefs[MIGRATION_VERSION_KEY] ?: 0
            if (currentVersion >= CURRENT_MIGRATION_VERSION) return@edit

            val storedLibrary = readLibrary(prefs)
            val items = storedLibrary.valueOr(emptyList())

            if (items == null) {
                backupCorrupt(prefs, LIBRARY_KEY, LIBRARY_BACKUP_KEY, storedLibrary)
                return@edit
            }

            val storedProgress = readLibraryProgress(prefs)
            val currentProgress = storedProgress.valueOr(emptyMap())

            if (currentProgress == null) {
                backupCorrupt(
                    prefs,
                    LIBRARY_PROGRESS_KEY,
                    LIBRARY_PROGRESS_BACKUP_KEY,
                    storedProgress
                )
                return@edit
            }

            val legacyProgress = items
                .associate { item -> item.id to item.progress() }
                .filterValues { !it.isEmpty }

            val mergedProgress = legacyProgress + currentProgress

            prefs[LIBRARY_KEY] = json.encodeToString(
                librarySerializer,
                items.map { it.withoutProgress() }
            )

            prefs[LIBRARY_PROGRESS_KEY] = json.encodeToString(
                libraryProgressSerializer,
                trimLibraryProgress(mergedProgress, MAX_TRACKED_LIBRARY_PROGRESS)
            )

            prefs[MIGRATION_VERSION_KEY] = CURRENT_MIGRATION_VERSION
            migrated = true
        }

        return migrated
    }

    suspend fun saveLibraryProgress(
        request: PlaybackRequest,
        positionMs: Long,
        nowMs: Long
    ): Boolean = updateLibraryProgress { current ->
        current + (
            request.key to LibraryProgressUpdater.applyProgress(
                current = current[request.key],
                request = request,
                positionMs = positionMs,
                nowMs = nowMs
            )
            )
    }

    suspend fun resetLibraryProgress(id: String): Boolean =
        updateLibraryProgress { current -> current - id }

    suspend fun forgetLibraryProgress(ids: Set<String>): Boolean =
        updateLibraryProgress { current -> current.filterKeys { it !in ids } }

    private suspend fun updateLibraryProgress(
        transform: (Map<String, LibraryProgress>) -> Map<String, LibraryProgress>
    ): Boolean {
        var written = false

        dataStore.edit { prefs ->
            val stored = readLibraryProgress(prefs)
            val current = stored.valueOr(emptyMap())

            if (current == null) {
                backupCorrupt(prefs, LIBRARY_PROGRESS_KEY, LIBRARY_PROGRESS_BACKUP_KEY, stored)
                return@edit
            }

            val updated = transform(current).filterValues { !it.isEmpty }
            prefs[LIBRARY_PROGRESS_KEY] = json.encodeToString(
                libraryProgressSerializer,
                trimLibraryProgress(updated, MAX_TRACKED_LIBRARY_PROGRESS)
            )
            written = true
        }

        return written
    }

    suspend fun saveLocalPosition(
        uri: String,
        positionMs: Long,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        var written = false
        val key = DeviceVideoKeys.canonical(uri)

        dataStore.edit { prefs ->
            val stored = readLocalProgress(prefs)
            val current = stored.valueOr(emptyMap())

            if (current == null) {
                backupCorrupt(prefs, LOCAL_POSITIONS_KEY, LOCAL_POSITIONS_BACKUP_KEY, stored)
                return@edit
            }

            val updated = current.toMutableMap()
            // Older versions keyed external opens by the raw provider uri; drop that alias so a
            // later rewind is not resurrected by the max-merge on read.
            if (key != uri) updated.remove(uri)
            if (positionMs <= 0L) {
                updated.remove(key)
            } else {
                updated[key] = LocalProgress(positionMs = positionMs, updatedAt = now)
            }

            prefs[LOCAL_POSITIONS_KEY] = json.encodeToString(
                progressSerializer,
                trimProgress(updated, MAX_TRACKED_POSITIONS)
            )
            written = true
        }

        return written
    }

    private fun readLibrary(prefs: Preferences): StoredValue<List<VideoItem>> {
        val raw = prefs[LIBRARY_KEY]
        if (raw.isNullOrBlank()) return StoredValue.Missing

        return runCatching { json.decodeFromString(librarySerializer, raw) }
            .fold(
                onSuccess = { items -> StoredValue.Loaded(items.map { it.normalized() }) },
                onFailure = { error -> StoredValue.Corrupt(raw, error) }
            )
    }

    private fun readLibraryProgress(prefs: Preferences): StoredValue<Map<String, LibraryProgress>> {
        val raw = prefs[LIBRARY_PROGRESS_KEY]
        if (raw.isNullOrBlank()) return StoredValue.Missing

        return runCatching { json.decodeFromString(libraryProgressSerializer, raw) }
            .fold(
                onSuccess = { StoredValue.Loaded(it) },
                onFailure = { error -> StoredValue.Corrupt(raw, error) }
            )
    }

    private fun readLocalProgress(prefs: Preferences): StoredValue<Map<String, LocalProgress>> {
        val raw = prefs[LOCAL_POSITIONS_KEY]
        if (raw.isNullOrBlank()) return StoredValue.Missing

        runCatching { json.decodeFromString(progressSerializer, raw) }
            .onSuccess { return StoredValue.Loaded(it) }

        return runCatching { json.decodeFromString(legacyPositionsSerializer, raw) }
            .fold(
                onSuccess = { legacy ->
                    StoredValue.Loaded(
                        legacy.mapValues { LocalProgress(positionMs = it.value, updatedAt = 0L) }
                    )
                },
                onFailure = { error -> StoredValue.Corrupt(raw, error) }
            )
    }

    private fun backupCorrupt(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        key: Preferences.Key<String>,
        backupKey: Preferences.Key<String>,
        stored: StoredValue<*>
    ) {
        val corrupt = stored as? StoredValue.Corrupt ?: return

        Log.e(
            TAG,
            "Refusing to overwrite unreadable data in ${key.name}; a backup was kept.",
            corrupt.cause
        )

        if (prefs[backupKey] == null) {
            prefs[backupKey] = corrupt.raw
            prefs[CORRUPTION_AT_KEY] = System.currentTimeMillis()
        }
    }

    private companion object {
        const val TAG = "LibraryRepository"

        val LIBRARY_KEY = stringPreferencesKey("library_json_v2")
        val LIBRARY_BACKUP_KEY = stringPreferencesKey("library_json_v2_corrupt_backup")
        val LIBRARY_PROGRESS_KEY = stringPreferencesKey("library_progress_json")
        val LIBRARY_PROGRESS_BACKUP_KEY =
            stringPreferencesKey("library_progress_json_corrupt_backup")
        val LOCAL_POSITIONS_KEY = stringPreferencesKey("local_positions_json")
        val LOCAL_POSITIONS_BACKUP_KEY =
            stringPreferencesKey("local_positions_json_corrupt_backup")
        val CORRUPTION_AT_KEY = longPreferencesKey("store_corruption_detected_at")
        val MIGRATION_VERSION_KEY = intPreferencesKey("library_migration_version")

        const val CURRENT_MIGRATION_VERSION = 1

        const val MAX_TRACKED_POSITIONS = 300
        const val MAX_TRACKED_LIBRARY_PROGRESS = 500
    }
}
