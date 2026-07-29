package com.opplayer.app.player

import com.opplayer.app.data.EpisodePattern

object LinkPatternDetector {

    enum class Failure { EMPTY, IDENTICAL, NOT_NUMERIC, NOT_INCREASING, TOO_LONG }

    sealed interface Result {
        data class Detected(val pattern: EpisodePattern) : Result
        data class Rejected(val failure: Failure) : Result
    }

    private const val MAX_DIGITS = 4
    private const val MAX_STEP = 5

    fun detect(firstUrl: String, secondUrl: String): Result {
        val first = firstUrl.trim()
        val second = secondUrl.trim()

        if (first.isEmpty() || second.isEmpty()) return Result.Rejected(Failure.EMPTY)
        if (first == second) return Result.Rejected(Failure.IDENTICAL)

        val limit = minOf(first.length, second.length)

        var head = 0
        while (head < limit && first[head] == second[head]) head++

        var tail = 0
        while (
            tail < limit - head &&
            first[first.length - 1 - tail] == second[second.length - 1 - tail]
        ) {
            tail++
        }

        var firstStart = head
        var secondStart = head
        var firstEnd = first.length - tail
        var secondEnd = second.length - tail

        while (
            firstStart > 0 && secondStart > 0 &&
            first[firstStart - 1].isDigit() && second[secondStart - 1].isDigit()
        ) {
            firstStart--
            secondStart--
        }

        while (
            firstEnd < first.length && secondEnd < second.length &&
            first[firstEnd].isDigit() && second[secondEnd].isDigit()
        ) {
            firstEnd++
            secondEnd++
        }

        val firstDigits = first.substring(firstStart, firstEnd)
        val secondDigits = second.substring(secondStart, secondEnd)

        if (firstDigits.isEmpty() || secondDigits.isEmpty()) {
            return Result.Rejected(Failure.NOT_NUMERIC)
        }
        if (firstDigits.any { !it.isDigit() } || secondDigits.any { !it.isDigit() }) {
            return Result.Rejected(Failure.NOT_NUMERIC)
        }
        if (firstDigits.length > MAX_DIGITS || secondDigits.length > MAX_DIGITS) {
            return Result.Rejected(Failure.TOO_LONG)
        }

        val prefix = first.substring(0, firstStart)
        val suffix = first.substring(firstEnd)

        if (second.substring(0, secondStart) != prefix || second.substring(secondEnd) != suffix) {
            return Result.Rejected(Failure.NOT_NUMERIC)
        }

        val firstValue = firstDigits.toIntOrNull() ?: return Result.Rejected(Failure.NOT_NUMERIC)
        val secondValue = secondDigits.toIntOrNull() ?: return Result.Rejected(Failure.NOT_NUMERIC)

        val step = secondValue - firstValue
        if (step <= 0 || step > MAX_STEP) return Result.Rejected(Failure.NOT_INCREASING)

        val pad = if (firstDigits.length == secondDigits.length) firstDigits.length else 1

        val pattern = EpisodePattern(
            prefix = prefix,
            suffix = suffix,
            episode = firstValue,
            pad = pad,
            step = step
        )

        val rebuildsBoth = pattern.url == first && pattern.next()?.url == second
        if (!rebuildsBoth) return Result.Rejected(Failure.NOT_NUMERIC)

        return Result.Detected(pattern)
    }
}
