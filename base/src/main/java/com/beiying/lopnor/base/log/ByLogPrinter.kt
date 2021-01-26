package com.beiying.lopnor.base.log

interface ByLogPrinter {
    fun print(config: ByLogConfig, level: Int, tag: String, printString: String)
}