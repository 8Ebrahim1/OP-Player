package com.opplayer.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.opplayer.app.player.PlaybackRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Repository tests against a real DataStore backed by a temporary file.
 *
 * These exist because the most dangerous bug in this layer was silent: a
 * corrupt blob decoded to an empty list and the next write made the loss
 * permanent. That can only be caught by driving the store itself.
 */
class LibraryRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private lateinit var directory: File
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: LibraryRepository

    private val libraryKey = stringPreferencesKey("library_json_v2")
    private val libraryBackupKey = stringPreferencesKey("library_json_v2_corrupt_backup")
    private val positionsKey = stringPreferencesKey("local_positions_json")
    private val progressKey = stringPreferencesKey("library_progress_json")
    private val migrationKey = intPreferencesKey("library_migration_version")

    private val request = PlaybackRequest(
        key = "item-1",
        title = "Show",
        uri = "https://cdn.test/s01e01.mp4",
        source = PlaybackRequest.Source.LIBRARY,
        episodeLabel = "E01"
    )

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("library-store").toFile()
        file = File(directory, "test.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(dispatcher),
            produceFile = { file }
        )
        repository = LibraryRepository(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun `an added item is read back`() = runTest(dispatcher) {
        repository.updateLibrary { it + VideoItem.create(title = "A", url = "https://a.test/a.mp4") }

        val library = repository.library.first()

        assertEquals(1, library.size)
        assertEquals("A", library.first().title)
    }

    @Test
    fun `a corrupt library is never overwritten`() = runTest(dispatcher) {
        dataStore.edit { it[libraryKey] = "{ this is not json" }

        val written = repository.updateLibrary { it + VideoItem.create("B", "https://b.test/b.mp4") }

        assertFalse(written)
        assertEquals("{ this is not json", dataStore.data.first()[libraryKey])
    }

    @Test
    fun `a corrupt library is backed up once`() = runTest(dispatcher) {
        dataStore.edit { it[libraryKey] = "broken" }

        repository.updateLibrary { it }
        repository.updateLibrary { it }

        assertEquals("broken", dataStore.data.first()[libraryBackupKey])
    }

    @Test
    fun `an empty store is not treated as damage`() = runTest(dispatcher) {
        val written = repository.updateLibrary { it + VideoItem.create("C", "https://c.test/c.mp4") }

        assertTrue(written)
        assertEquals(1, repository.library.first().size)
    }

    @Test
    fun `progress is stored outside the library blob`() = runTest(dispatcher) {
        repository.updateLibrary {
            it + VideoItem(id = "item-1", title = "Show", url = "https://cdn.test/s01e01.mp4")
        }
        val libraryJsonBefore = dataStore.data.first()[libraryKey]

        repository.saveLibraryProgress(request, positionMs = 42_000L, nowMs = 1_000L)

        assertEquals(libraryJsonBefore, dataStore.data.first()[libraryKey])
        assertEquals(42_000L, repository.library.first().first().positionMs)
    }

    @Test
    fun `resetting progress clears only the position`() = runTest(dispatcher) {
        repository.updateLibrary {
            it + VideoItem(id = "item-1", title = "Show", url = "https://cdn.test/s01e01.mp4")
        }
        repository.saveLibraryProgress(request, positionMs = 42_000L, nowMs = 1_000L)

        repository.resetLibraryProgress("item-1")

        val item = repository.library.first().single()
        assertEquals(0L, item.positionMs)
        assertEquals("Show", item.title)
    }

    @Test
    fun `deleting an item forgets its progress`() = runTest(dispatcher) {
        repository.updateLibrary {
            it + VideoItem(id = "item-1", title = "Show", url = "https://cdn.test/s01e01.mp4")
        }
        repository.saveLibraryProgress(request, positionMs = 42_000L, nowMs = 1_000L)

        repository.updateLibrary { current -> current.filterNot { it.id == "item-1" } }
        repository.forgetLibraryProgress(setOf("item-1"))

        repository.updateLibrary {
            it + VideoItem(id = "item-1", title = "Show", url = "https://cdn.test/s01e01.mp4")
        }

        assertEquals(0L, repository.library.first().single().positionMs)
    }

    @Test
    fun `a legacy item without an id keeps the same identity across reads`() =
        runTest(dispatcher) {
            dataStore.edit {
                it[libraryKey] =
                    """[{"title":"Legacy","url":"https://legacy.test/v.mp4"}]"""
            }

            val first = repository.library.first().single().id
            val second = repository.library.first().single().id

            assertTrue(first.startsWith("legacy-"))
            assertEquals(first, second)
        }

    @Test
    fun `a legacy episode pattern with a zero step is repaired instead of crashing`() =
        runTest(dispatcher) {
            dataStore.edit {
                it[libraryKey] = """[{
                    "id":"item-1",
                    "title":"Legacy",
                    "url":"https://legacy.test/v01.mp4",
                    "pattern":{
                        "prefix":"https://legacy.test/v",
                        "suffix":".mp4",
                        "episode":1,
                        "pad":0,
                        "step":0
                    }
                }]"""
            }

            val pattern = repository.library.first().single().pattern

            assertNotNull(pattern)
            assertEquals(1, pattern!!.step)
            assertEquals(1, pattern.pad)
            assertEquals(2, pattern.next()?.episode)
        }

    @Test
    fun `device positions survive the legacy number format`() = runTest(dispatcher) {
        dataStore.edit { it[positionsKey] = """{"content://video/1":15000}""" }

        assertEquals(15_000L, repository.localPositions.first()["content://video/1"])
    }

    @Test
    fun `a corrupt position store is not overwritten`() = runTest(dispatcher) {
        dataStore.edit { it[positionsKey] = "not json at all" }

        val written = repository.saveLocalPosition("content://video/1", 1_000L)

        assertFalse(written)
        assertEquals("not json at all", dataStore.data.first()[positionsKey])
    }

    @Test
    fun `clearing a device position removes the entry`() = runTest(dispatcher) {
        repository.saveLocalPosition("content://video/1", 5_000L)
        repository.saveLocalPosition("content://video/1", 0L)

        assertNull(repository.localPositions.first()["content://video/1"])
    }

    @Test
    fun `migration preserves legacy playback progress`() = runTest(dispatcher) {
        dataStore.edit {
            it[libraryKey] = """[{
                "id":"item-1",
                "title":"Show",
                "url":"https://cdn.test/s01e01.mp4",
                "positionMs":42000,
                "lastPlayedAt":1700000000000,
                "currentUrl":"https://cdn.test/s01e03.mp4",
                "currentLabel":"S01E03"
            }]"""
        }

        assertTrue(repository.migrateLegacyDataIfNeeded())

        val item = repository.library.first().single()
        assertEquals(42_000L, item.positionMs)
        assertEquals(1_700_000_000_000L, item.lastPlayedAt)
        assertEquals("https://cdn.test/s01e03.mp4", item.currentUrl)
        assertEquals("S01E03", item.currentLabel)
    }

    @Test
    fun `migration moves progress out of the library blob`() = runTest(dispatcher) {
        dataStore.edit {
            it[libraryKey] = """[{
                "id":"item-1",
                "title":"Show",
                "url":"https://cdn.test/s01e01.mp4",
                "positionMs":42000
            }]"""
        }

        repository.migrateLegacyDataIfNeeded()

        val storedLibrary = dataStore.data.first()[libraryKey].orEmpty()
        assertFalse(storedLibrary.contains("42000"))
        assertTrue(dataStore.data.first()[progressKey].orEmpty().contains("42000"))
    }

    @Test
    fun `migration runs only once`() = runTest(dispatcher) {
        repository.updateLibrary {
            it + VideoItem(id = "item-1", title = "Show", url = "https://cdn.test/s01e01.mp4")
        }

        assertTrue(repository.migrateLegacyDataIfNeeded())
        assertFalse(repository.migrateLegacyDataIfNeeded())
    }

    @Test
    fun `migration keeps a position written in the new format`() = runTest(dispatcher) {
        dataStore.edit {
            it[libraryKey] = """[{
                "id":"item-1",
                "title":"Show",
                "url":"https://cdn.test/s01e01.mp4",
                "positionMs":42000
            }]"""
        }
        repository.saveLibraryProgress(request, positionMs = 90_000L, nowMs = 5_000L)

        repository.migrateLegacyDataIfNeeded()

        assertEquals(90_000L, repository.library.first().single().positionMs)
    }

    @Test
    fun `a corrupt store neither migrates nor records the migration`() = runTest(dispatcher) {
        dataStore.edit { it[libraryKey] = "not json" }

        assertFalse(repository.migrateLegacyDataIfNeeded())
        assertEquals("not json", dataStore.data.first()[libraryKey])
        assertNull(dataStore.data.first()[migrationKey])

        // The next launch must try again, so the marker cannot hide the damage.
        dataStore.edit {
            it[libraryKey] = """[{
                "id":"item-1",
                "title":"Show",
                "url":"https://cdn.test/s01e01.mp4",
                "positionMs":7000
            }]"""
        }

        assertTrue(repository.migrateLegacyDataIfNeeded())
        assertEquals(7_000L, repository.library.first().single().positionMs)
    }
}
