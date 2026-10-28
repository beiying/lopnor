package com.beiying.common.flutter

import android.content.Context
import android.os.Looper
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.view.FlutterMain

/**
 * Flutter提升加载速度，实现秒开Flutter模块
 * 1、如何让预加载不损失首页性能；
 * 2、如何实例化多个Flutter引擎并分别加载不同的dart入口
 * */
class ByFlutterCacheManager private constructor() {
    //预加载Flutter
    fun preload(context: Context) {
        Looper.myQueue().addIdleHandler {
            initFlutterEngine(context, "main")
            initFlutterEngine(context, "second")
            false
        }
    }

    fun getCachedFlutterEngine(moduleName: String, context: Context?): FlutterEngine {
        var engine: FlutterEngine? = FlutterEngineCache.getInstance()[moduleName]
        if (engine == null && context != null) {
            engine = initFlutterEngine(context, moduleName)
        }
        return engine!!
    }
    private fun initFlutterEngine(context: Context, moduleName: String): FlutterEngine {
        val flutterEngine: FlutterEngine = FlutterEngine(context)
        //Flutter插件的注册要紧跟引擎初始化之后，否则会有在dart中调用插件时因为还未完成初始化而导致的时序问题
        ByFlutterBridge.init(flutterEngine)
        flutterEngine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint(FlutterMain.findAppBundlePath(), moduleName))
        FlutterEngineCache.getInstance().put(moduleName, flutterEngine)
        return flutterEngine
    }
    companion object {
        @JvmStatic
        @get:Synchronized
        var instance: ByFlutterCacheManager? = null
        get() {
            if (instance == null) {
                instance = ByFlutterCacheManager()
            }
            return instance
        }
        private set
    }
}