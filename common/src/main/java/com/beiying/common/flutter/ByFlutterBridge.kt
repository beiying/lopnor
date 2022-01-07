//package com.beiying.common.flutter
//
//import android.content.Context
//import com.beiying.common.IByBridge
//import io.flutter.embedding.engine.FlutterEngine
//import io.flutter.plugin.common.MethodCall
//import io.flutter.plugin.common.MethodChannel
//
///**
// * Flutter和Native通信插件
// * */
//class ByFlutterBridge : IByBridge<Any?, MethodChannel.Result>, MethodChannel.MethodCallHandler {
//    // 由于存在多个FlutterEngine，每个FlutterEngine都需要单独注册一个MethodChannel,所以需要用集合保存所有的MethodChannel
//    private var methodChannels = mutableListOf<MethodChannel>()
//    companion object {
//        @JvmStatic
//        var instance: ByFlutterBridge? = null
//        private set
//
//        @JvmStatic
//        fun init(flutterEngine: FlutterEngine): ByFlutterBridge? {
//            val methodChannel: MethodChannel = MethodChannel(flutterEngine.dartExecutor, "ByFlutterBridge")
//            if (instance == null) {
//                instance = ByFlutterBridge()
//            }
//            methodChannel.setMethodCallHandler(instance)
//            instance?.apply {
//                methodChannels.add(methodChannel)
//            }
//            return instance
//        }
//    }
//
//    /**
//     * Native向Flutter发送消息
//     * */
//    fun fire(method: String, arguments: Any?) {
//        methodChannels.forEach {
//            it.invokeMethod(method, arguments)
//        }
//    }
//
//    fun fire(method: String, arguments: Any?, callback: MethodChannel.Result) {
//        methodChannels.forEach {
//            it.invokeMethod(method, arguments, callback)
//        }
//    }
//
//    override fun onBack(p: Any?) {
//        TODO("Not yet implemented")
//    }
//
//    override fun gotoNative(p: Any?) {
//        if (p is Map<*, *>) {
//            val action = p["action"]
//            if (action == "goToDetail") {
//                val goodsId = p["goodsId"]
////                ARouter.getInstance().build("/detail/main").witheString("goodsId", goodsId as String?).navigation()
//            }
//        }
//    }
//
//    override fun getHeaderParams(callback: MethodChannel.Result) {
//        TODO("Not yet implemented")
//    }
//
//    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
//        when(call.method) {
//            "onBack" -> onBack(call.arguments)
//            "gotoNative" -> gotoNative(call.arguments)
//            "getHeaderParams" -> getHeaderParams(result)
//            else -> result.notImplemented()
//        }
//    }
//
//}