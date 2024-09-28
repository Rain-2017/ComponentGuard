package com.rain.componentguard.utils

object LogProguard {

    private const val DEBUG = true
    fun log(log: String) {
        if (DEBUG) {
            println(log)
        }
    }
}