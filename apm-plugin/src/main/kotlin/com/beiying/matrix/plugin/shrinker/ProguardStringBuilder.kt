package com.beiying.matrix.plugin.shrinker

import java.util.*

class ProguardStringBuilder {
    private val alphaArray = CharArray(26)
    private val numberArray = CharArray(10)

    private val WIN_INVALID_FILE_NAME =
        Arrays.asList(
            "aux", "nul", "prn", "nul", "con",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
        )


    private var alphaIndex = 0
    private var numberIndex = 0
    private var prefix = ""

    fun ProguardStringBuilder() {
        var i = 0
        var alpha = 'a'
        while (alpha <= 'z') {
            alphaArray[i++] = alpha
            alpha++
        }
        i = 0
        var num = '0'
        while (num <= '9') {
            numberArray[i++] = num
            num++
        }
    }

    private fun getPrefix(len: Int): String {
        return if (len == 0) {
            "" + alphaArray[0]
        } else if (len == 1) {
            val curChar = prefix[0]
            if (curChar < alphaArray[alphaArray.size - 1]) {
                "" + (curChar.toInt() + 1).toChar()
            } else {
                "" + alphaArray[0] + alphaArray[0]
            }
        } else {
            val lastChar = prefix[len - 1]
            if (lastChar == alphaArray[alphaArray.size - 1]) {
                prefix.substring(0, len - 1) + numberArray[0]
            } else if (lastChar == numberArray[numberArray.size - 1]) {
                getPrefix(len - 1) + alphaArray[0]
            } else {
                prefix.substring(0, len - 1) + (lastChar.toInt() + 1).toChar()
            }
        }
    }

    private fun nextTurn() {
        alphaIndex = 0
        numberIndex = 0
        prefix = getPrefix(prefix.length)
    }

    fun generateNextProguard(): String {
        var result = ""
        result = if (prefix == "") {
            if (alphaIndex <= alphaArray.size - 1) {
                alphaArray[alphaIndex++].toString()
            } else {
                nextTurn()
                prefix + alphaArray[alphaIndex++]
            }
        } else {
            if (alphaIndex <= alphaArray.size - 1) {
                prefix + alphaArray[alphaIndex++]
            } else if (numberIndex <= numberArray.size - 1) {
                prefix + numberArray[numberIndex++]
            } else {
                nextTurn()
                prefix + alphaArray[alphaIndex++]
            }
        }
        //System.out.println(prefix + "," + result)
        return result
    }

    fun generateNextProguardFileName(): String? {
        var result = generateNextProguard()
        while (WIN_INVALID_FILE_NAME.contains(result.toLowerCase())) {
            result = generateNextProguard()
        }
        return result
    }

    fun reset() {
        alphaIndex = 0
        numberIndex = 0
        prefix = ""
    }
}