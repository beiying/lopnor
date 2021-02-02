package com.beiying.lopnor.base.log

import java.text.SimpleDateFormat
import java.util.*

class ByLogModel(var timeMillis: Long, var level: Int, var tag: String, var log: String) {
    companion object {
        val sdf: SimpleDateFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.CHINA)
    }

    fun format(timeMillis: Long): String {
        return sdf.format(timeMillis)
    }

    fun getFlattend(): String {
        return "${format(timeMillis)} | $level | $tag |:"
    }

    fun flattendLog(): String {
        return "${getFlattend()} \n $log"
    }

}