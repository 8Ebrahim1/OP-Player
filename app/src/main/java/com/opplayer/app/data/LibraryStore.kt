package com.opplayer.app.data

sealed interface StoredValue<out T> {

    data class Loaded<T>(val value: T) : StoredValue<T>

    data object Missing : StoredValue<Nothing>

    data class Corrupt(val raw: String, val cause: Throwable) : StoredValue<Nothing>
}

fun <T> StoredValue<T>.valueOr(empty: T): T? = when (this) {
    is StoredValue.Loaded -> value
    StoredValue.Missing -> empty
    is StoredValue.Corrupt -> null
}
