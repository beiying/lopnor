package com.beiying.lopnor.base.log

interface ByLogFormatter<T> {
    fun format(data: T): String
}