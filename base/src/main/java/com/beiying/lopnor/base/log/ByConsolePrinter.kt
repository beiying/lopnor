package com.beiying.lopnor.base.log

import android.util.Log
import com.beiying.lopnor.base.log.ByLogConfig.Companion.MAX_LEN

class ByConsolePrinter: ByLogPrinter {
    override fun print(config: ByLogConfig, level: Int, tag: String, printString: String) {
        val len = printString.length
        var lineCount = len / MAX_LEN
        if (lineCount > 0) {
            var index: Int = 0
            for(line in 0 until lineCount) {
                Log.println(level, tag, printString.substring(index, index + MAX_LEN))
                index += MAX_LEN
            }

            if (index != len) {
                Log.println(level, tag, printString.substring(index, len))
            }
        } else {
            Log.println(level, tag, printString)
        }
    }
}