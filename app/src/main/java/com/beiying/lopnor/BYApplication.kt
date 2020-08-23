package com.beiying.lopnor

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

class BYApplication : MultiDexApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}