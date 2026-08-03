package com.opplayer.app.player.subtitle

data class EmbeddedTrackInfo(
    val index: Int,
    val language: String?,
    val selected: Boolean
)

data class SubtitleOptionItem(
    val id: String,
    val label: String,
    val selected: Boolean
)

sealed interface SubtitleChoice {
    data object Off : SubtitleChoice
    data object CurrentExternal : SubtitleChoice
    data class ExternalFile(val index: Int) : SubtitleChoice
    data class EmbeddedTrack(val index: Int) : SubtitleChoice
}

object SubtitleOptions {

    const val ID_OFF = "off"
    const val ID_CURRENT = "current"
    const val PREFIX_FILE = "file:"
    const val PREFIX_TRACK = "track:"

    fun build(
        disabled: Boolean,
        usingExternal: Boolean,
        externalName: String?,
        externalUri: String?,
        candidates: List<SubtitleFileCandidate>,
        embeddedTracks: List<EmbeddedTrackInfo>,
        offLabel: String,
        embeddedLabel: String
    ): List<SubtitleOptionItem> = buildList {
        add(SubtitleOptionItem(id = ID_OFF, label = offLabel, selected = disabled))

        if (externalName != null) {
            add(
                SubtitleOptionItem(
                    id = ID_CURRENT,
                    label = externalName,
                    selected = !disabled && usingExternal
                )
            )
        }

        candidates.forEachIndexed { index, candidate ->
            if (candidate.uri != externalUri) {
                add(
                    SubtitleOptionItem(
                        id = PREFIX_FILE + index,
                        label = candidate.name,
                        selected = false
                    )
                )
            }
        }

        embeddedTracks.forEach { track ->
            val language = track.language?.takeIf { it.isNotBlank() }
            val suffix = if (language == null) "" else " (" + language + ")"

            add(
                SubtitleOptionItem(
                    id = PREFIX_TRACK + track.index,
                    label = embeddedLabel + " " + (track.index + 1) + suffix,
                    selected = !disabled && !usingExternal && track.selected
                )
            )
        }
    }

    fun parse(id: String): SubtitleChoice? = when {
        id == ID_OFF -> SubtitleChoice.Off
        id == ID_CURRENT -> SubtitleChoice.CurrentExternal

        id.startsWith(PREFIX_FILE) ->
            id.removePrefix(PREFIX_FILE).toIntOrNull()?.let { SubtitleChoice.ExternalFile(it) }

        id.startsWith(PREFIX_TRACK) ->
            id.removePrefix(PREFIX_TRACK).toIntOrNull()?.let { SubtitleChoice.EmbeddedTrack(it) }

        else -> null
    }
}
