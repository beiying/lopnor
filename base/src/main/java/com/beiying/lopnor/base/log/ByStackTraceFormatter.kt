package com.beiying.lopnor.base.log

import java.lang.StringBuilder

class ByStackTraceFormatter: ByLogFormatter<Array<StackTraceElement>> {
    override fun format(data: Array<StackTraceElement>): String {
        var sb: StringBuilder = StringBuilder()
        if (data.size == 1) {
            return "\t- ${data[0].toString()}"
        } else {
            data.forEachIndexed { index, stackTraceElement ->
                if (index == 0) {
                    sb.append("stackTrace: \n")
                }
                if (index != data.size - 1) {
                    sb.append("\t|-")
                    sb.append(stackTraceElement.toString())
                    sb.append("\n")
                } else {
                    sb.append("\tL")
                    sb.append(stackTraceElement.toString())
                }
            }
        }
        return sb.toString()
    }
}