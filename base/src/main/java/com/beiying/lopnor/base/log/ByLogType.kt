package com.beiying.lopnor.base.log

import android.util.Log
import androidx.annotation.IntDef
import java.lang.annotation.RetentionPolicy

class ByLogType {
    companion object {
        const val V: Int = Log.VERBOSE
        const val D: Int = Log.DEBUG
        const val I: Int = Log.INFO
        const val W: Int = Log.WARN
        const val E: Int = Log.ERROR
        const val A: Int = Log.ASSERT

    }

    @IntDef(V, D, I, W, E, A)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TYPE
}