package com.rain.componentguard.utils


/**
 * User: rain
 * Date: 2023/7/26
 */

object StringUtil {
    fun generateWords(size: Int): String {
        val nonceScope = "abcdefghijklmnopqrstuvwxyz"
        val scopeSize = nonceScope.length
        val nonceItem: (Int) -> Char = { nonceScope[(scopeSize * Math.random()).toInt()] }
        return Array(size, nonceItem).joinToString("")
    }

    fun generateNonce(size: Int): String {
        val nonceScope = "1234567890abcdefghijklmnopqrstuvwxyz_"
        val scopeSize = nonceScope.length
        val nonceItem: (Int) -> Char = { nonceScope[(scopeSize * Math.random()).toInt()] }
        return Array(size, nonceItem).joinToString("")
    }
}

internal fun String.splitWords(): List<String> {
    val regex = Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
    return split(regex).map { it.lowercase() }
}

