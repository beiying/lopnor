package com.beiying.common

import android.app.Application

class ByBaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()
//        ByFlutterCacheManager.instance?.preload(this)
    }
}