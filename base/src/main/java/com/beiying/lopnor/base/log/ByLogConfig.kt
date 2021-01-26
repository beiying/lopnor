package com.beiying.lopnor.base.log

abstract class ByLogConfig {
    fun getGlobalTag(): String {
        return "beiying"
    }

    fun enable(): Boolean {
        return true
    }

    fun injectJsonParser(): JsonParser? {
        return null
    }

    fun includeThread(): Boolean {
        return false
    }

    fun stackTraceDepth(): Int {
        return 5
    }

    fun printers(): Array<ByLogPrinter>? {
        return null
    }

    companion object {
        const val MAX_LEN: Int = 512
        val BY_THREAD_FORMATTER: ByThreadFormatter = ByThreadFormatter()
        val BY_STACK_TRACE_FORMATTER: ByStackTraceFormatter = ByStackTraceFormatter()
    }

    interface JsonParser {
        fun toJson(src: Any): String
    }
}