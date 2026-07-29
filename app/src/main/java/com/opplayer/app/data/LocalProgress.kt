package com.opplayer.app.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalProgress(
    val positionMs: Long,
    val updatedAt: Long = 0L
)

fun trimProgress(
    entries: Map<String, LocalProgress>,
    limit: Int
): Map<String, LocalProgress> {
    if (limit <= 0) return emptyMap()
    if (entries.size <= limit) return entries

    return entries.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, LocalProgress>> { it.value.updatedAt }
                .thenByDescending { it.value.positionMs }
                .thenBy { it.key }
        )
        .take(limit)
        .associate { it.key to it.value }
}
