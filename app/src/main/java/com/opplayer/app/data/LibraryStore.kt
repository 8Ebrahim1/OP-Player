package com.opplayer.app.data

/**
 * Outcome of reading one persisted JSON blob.
 *
 * The three cases exist so that "there is nothing stored yet" can never be
 * confused with "the stored data could not be parsed". Collapsing the two is
 * what allowed a corrupt blob to be silently replaced by an empty library.
 */
sealed interface StoredValue<out T> {

    /** The blob was present and parsed successfully. */
    data class Loaded<T>(val value: T) : StoredValue<T>

    /** Nothing has ever been written under this key. */
    data object Missing : StoredValue<Nothing>

    /** A blob is present but could not be parsed; [raw] is kept for the backup. */
    data class Corrupt(val raw: String, val cause: Throwable) : StoredValue<Nothing>
}

/** Value to work with when the store is readable, or null when it is corrupt. */
fun <T> StoredValue<T>.valueOr(empty: T): T? = when (this) {
    is StoredValue.Loaded -> value
    StoredValue.Missing -> empty
    is StoredValue.Corrupt -> null
}
