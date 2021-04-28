package com.beiying.common

import android.app.Application
import com.beiying.common.flutter.ByFlutterCacheManager

class ByBaseApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        ByFlutterCacheManager.instance?.preload(this)
    }
}