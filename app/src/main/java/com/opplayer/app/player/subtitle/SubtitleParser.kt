package com.opplayer.app.player.subtitle

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Parser for external subtitle files (SRT, WebVTT, ASS/SSA).
 *
 * Handles the two things that break Persian subtitles most often:
 * - non UTF-8 encodings (windows-1256 is very common for Farsi .srt files)
 * - centisecond timestamps used by ASS/SSA
 */
object SubtitleParser {

    private val LONG_TIME = Regex("""(\d{1,3}):(\d{1,2}):(\d{1,2})[.,](\d{1,3})""")
    private val SHORT_TIME = Regex("""(\d{1,3}):(\d{1,2})[.,](\d{1,3})""")
    private val HTML_TAG = Regex("<[^>]*>")
    private val BRACE_TAG = Regex("""\{[^}]*}""")
    private val BLANK_LINE = Regex("\n[ \t]*\n")

    val SUPPORTED_EXTENSIONS = listOf("srt", "vtt", "ass", "ssa", "txt")

    fun parse(fileName: String, bytes: ByteArray): List<SubtitleCue> {
        val text = decode(bytes)
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val cues = if (extension == "ass" || extension == "ssa") {
            parseAss(text)
        } else {
            parseSrtLike(text)
        }

        return cues
            .filter { it.text.isNotBlank() && it.endMs > it.startMs }
            .sortedBy { it.startMs }
    }

    /** Best-effort charset detection: BOM, then strict UTF-8, then Persian/Arabic legacy pages. */
    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        strictDecode(bytes, Charsets.UTF_8)?.let { return it }

        for (name in listOf("windows-1256", "windows-1250", "ISO-8859-1")) {
            val charset = runCatching { Charset.forName(name) }.getOrNull() ?: continue
            strictDecode(bytes, charset)?.let { return it }
        }

        return String(bytes, Charsets.UTF_8)
    }

    private fun strictDecode(bytes: ByteArray, charset: Charset): String? = runCatching {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()

    private fun parseSrtLike(raw: String): List<SubtitleCue> {
        val normalized = raw.replace("\r\n", "\n").replace('\r', '\n')
        val cues = mutableListOf<SubtitleCue>()

        for (block in normalized.split(BLANK_LINE)) {
            val lines = block.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            val timeIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeIndex < 0) continue

            val bounds = parseTimeLine(lines[timeIndex]) ?: continue
            val body = cleanText(lines.drop(timeIndex + 1).joinToString("\n"))
            if (body.isEmpty()) continue

            cues += SubtitleCue(bounds.first, bounds.second, body)
        }

        return cues
    }

    private fun parseAss(raw: String): List<SubtitleCue> {
        val lines = raw.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val cues = mutableListOf<SubtitleCue>()

        var startIndex = 1
        var endIndex = 2
        var textIndex = 9

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("Format:", ignoreCase = true) &&
                trimmed.contains("Text", ignoreCase = true)
            ) {
                val fields = trimmed.substringAfter(':')
                    .split(',')
                    .map { it.trim().lowercase() }

                fields.indexOf("start").takeIf { it >= 0 }?.let { startIndex = it }
                fields.indexOf("end").takeIf { it >= 0 }?.let { endIndex = it }
                fields.indexOf("text").takeIf { it >= 0 }?.let { textIndex = it }
                continue
            }

            if (!trimmed.startsWith("Dialogue:", ignoreCase = true)) continue

            val fields = trimmed.substringAfter(':').trim().split(',', limit = textIndex + 1)
            if (fields.size <= textIndex) continue

            val start = parseTime(fields.getOrNull(startIndex) ?: "") ?: continue
            val end = parseTime(fields.getOrNull(endIndex) ?: "") ?: continue

            val body = cleanText(
                fields[textIndex]
                    .replace("\\N", "\n", ignoreCase = true)
                    .replace("\\h", " ", ignoreCase = true)
            )
            if (body.isEmpty()) continue

            cues += SubtitleCue(start, end, body)
        }

        return cues
    }

    private fun cleanText(input: String): String =
        BRACE_TAG.replace(input, "")
            .let { HTML_TAG.replace(it, "") }
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\u200f", "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private fun parseTimeLine(line: String): Pair<Long, Long>? {
        val parts = line.split("-->")
        if (parts.size < 2) return null

        val start = parseTime(parts[0]) ?: return null
        val end = parseTime(parts[1]) ?: return null
        return start to end
    }

    fun parseTime(input: String): Long? {
        LONG_TIME.find(input)?.let { match ->
            val (hours, minutes, seconds, fraction) = match.destructured
            return hours.toLong() * 3_600_000L +
                minutes.toLong() * 60_000L +
                seconds.toLong() * 1_000L +
                fractionToMillis(fraction)
        }

        SHORT_TIME.find(input)?.let { match ->
            val (minutes, seconds, fraction) = match.destructured
            return minutes.toLong() * 60_000L +
                seconds.toLong() * 1_000L +
                fractionToMillis(fraction)
        }

        return null
    }

    private fun fractionToMillis(value: String): Long = when (value.length) {
        1 -> value.toLong() * 100L
        2 -> value.toLong() * 10L
        else -> value.take(3).toLong()
    }
}
