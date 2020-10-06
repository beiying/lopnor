package com.beiying.lopnor.base

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.opengl.GLES20
import android.os.Build

class OpenglUtil {
    companion object {
        fun isSupportEs2(context: Context): Boolean {
            val activityManager: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager?.let {
                val deviceConfig: ConfigurationInfo = activityManager.deviceConfigurationInfo
                val reqGlEsVersion: Int = deviceConfig.reqGlEsVersion

                return reqGlEsVersion >= GLES20.GL_VERSION || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                        && (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK build for x86")))
            }
            return false
        }
    }
}