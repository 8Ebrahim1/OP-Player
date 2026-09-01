package com.opplayer.app.data

/**
 * Orders file names the way a viewer reads them: the name stem is compared before the
 * extension, so "clip.mkv" stays next to "clip part 2.mkv" instead of the dot losing to the
 * space. Digit runs compare numerically so "077" follows "076" and "Episode 9" stays before
 * "Episode 10", while text compares case-insensitively.
 */
object NaturalNameOrder : Comparator<String> {

    override fun compare(left: String, right: String): Int {
        val leftStemEnd = extensionStart(left)
        val rightStemEnd = extensionStart(right)

        val stemDiff = compareNatural(left, 0, leftStemEnd, right, 0, rightStemEnd)
        if (stemDiff != 0) return stemDiff

        return compareNatural(left, leftStemEnd, left.length, right, rightStemEnd, right.length)
    }

    /** The final dot starts the extension, except in dotfiles where it is part of the name. */
    private fun extensionStart(value: String): Int {
        val dot = value.lastIndexOf('.')
        return if (dot > 0) dot else value.length
    }

    private fun compareNatural(
        left: String,
        leftStart: Int,
        leftEnd: Int,
        right: String,
        rightStart: Int,
        rightEnd: Int
    ): Int {
        var leftIndex = leftStart
        var rightIndex = rightStart

        while (leftIndex < leftEnd && rightIndex < rightEnd) {
            val leftChar = left[leftIndex]
            val rightChar = right[rightIndex]

            if (leftChar.isDigit() && rightChar.isDigit()) {
                val leftRunEnd = digitRunEnd(left, leftIndex)
                val rightRunEnd = digitRunEnd(right, rightIndex)

                val leftDigits = left.substring(leftIndex, leftRunEnd).trimStart('0')
                val rightDigits = right.substring(rightIndex, rightRunEnd).trimStart('0')

                if (leftDigits.length != rightDigits.length) {
                    return leftDigits.length - rightDigits.length
                }

                val digitDiff = leftDigits.compareTo(rightDigits)
                if (digitDiff != 0) return digitDiff

                leftIndex = leftRunEnd
                rightIndex = rightRunEnd
                continue
            }

            val charDiff = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
            if (charDiff != 0) return charDiff

            leftIndex++
            rightIndex++
        }

        return (leftEnd - leftIndex) - (rightEnd - rightIndex)
    }

    private fun digitRunEnd(value: String, start: Int): Int {
        var end = start
        while (end < value.length && value[end].isDigit()) end++
        return end
    }
}
