package com.beiying.lopnor.base.log

import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.SimpleFormatter

object CrashHandler {
    fun init() {
        Thread.setDefaultUncaughtExceptionHandler(UncaughtCrashHandler())
    }

    class UncaughtCrashHandler: Thread.UncaughtExceptionHandler {
        private val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINA)
        private val LAUNCH_TIME = formatter.format(Date())
        private val defaultUncaughtCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        override fun uncaughtException(t: Thread, e: Throwable) {
            if (!handleException(e) && defaultUncaughtCrashHandler != null) {
                defaultUncaughtCrashHandler.uncaughtException(t, e)
            }
            restartApp()
        }

        private fun handleException(e: Throwable?): Boolean {
            if (e == null) return false
            val log = collectDeviceInfo()
            saveCrashInfo2File(log)
            return true
        }

        private fun saveCrashInfo2File(log: String) {

        }

        private fun restartApp() {
        }

        private fun collectDeviceInfo(): String {
            val sb = StringBuffer()
            return sb.toString()
        }

    }
}