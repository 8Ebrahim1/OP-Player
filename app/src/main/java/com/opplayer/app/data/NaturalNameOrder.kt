package com.opplayer.app.data

/**
 * Orders file names the way a viewer reads them: digit runs compare numerically so "077" follows
 * "076" and "Episode 9" stays before "Episode 10", while text compares case-insensitively.
 */
object NaturalNameOrder : Comparator<String> {

    override fun compare(left: String, right: String): Int {
        val leftDot = left.lastIndexOf('.')
        val rightDot = right.lastIndexOf('.')

        // Compare the name stems first: a shorter stem sorts before a longer name that
        // extends it ("clip.mkv" before "clip part 2.mkv") instead of letting the
        // extension dot order them ('.' > ' '). Equal stems fall back to the full name.
        if (leftDot > 0 && rightDot > 0) {
            val stemDiff = compareNames(left.substring(0, leftDot), right.substring(0, rightDot))
            if (stemDiff != 0) return stemDiff
        }

        return compareNames(left, right)
    }

    private fun compareNames(left: String, right: String): Int {
        var leftIndex = 0
        var rightIndex = 0

        while (leftIndex < left.length && rightIndex < right.length) {
            val leftChar = left[leftIndex]
            val rightChar = right[rightIndex]

            if (leftChar.isDigit() && rightChar.isDigit()) {
                val leftEnd = digitRunEnd(left, leftIndex)
                val rightEnd = digitRunEnd(right, rightIndex)

                val leftDigits = left.substring(leftIndex, leftEnd).trimStart('0')
                val rightDigits = right.substring(rightIndex, rightEnd).trimStart('0')

                if (leftDigits.length != rightDigits.length) {
                    return leftDigits.length - rightDigits.length
                }

                val digitDiff = leftDigits.compareTo(rightDigits)
                if (digitDiff != 0) return digitDiff

                leftIndex = leftEnd
                rightIndex = rightEnd
                continue
            }

            val charDiff = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
            if (charDiff != 0) return charDiff

            leftIndex++
            rightIndex++
        }

        return (left.length - leftIndex) - (right.length - rightIndex)
    }

    private fun digitRunEnd(value: String, start: Int): Int {
        var end = start
        while (end < value.length && value[end].isDigit()) end++
        return end
    }
}
