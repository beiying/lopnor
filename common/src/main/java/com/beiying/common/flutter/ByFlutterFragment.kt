//package com.beiying.common.flutter
//
//import android.content.Context
//import android.os.Bundle
//import android.view.View
//import android.view.ViewGroup
//import com.beiying.common.ByBaseFragment
//import com.beiying.common.R
//import io.flutter.embedding.android.FlutterTextureView
//import io.flutter.embedding.android.FlutterView
//import io.flutter.embedding.engine.FlutterEngine
//import io.flutter.embedding.engine.dart.DartExecutor
//import kotlinx.android.synthetic.main.fragment_flutter.*
//
///**
// * Android原生应用集成FlutterView
// * */
//abstract class ByFlutterFragment: ByBaseFragment() {
//    var flutterEngine: FlutterEngine? = null
//    var flutterView: FlutterView? = null
//    abstract val moduleName: String?
//
//    init {
//        flutterEngine = ByFlutterCacheManager.instance?.getCachedFlutterEngine(moduleName!!, context)
//    }
//
//    override fun getLayoutId(): Int {
//        return R.layout.fragment_flutter
//    }
//
//    fun setTitle(titleStr: String) {
//        title.text = titleStr
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        (layoutView as ViewGroup).addView(createFlutterView(activity!!))
//    }
//
//    fun createFlutterView(context: Context):FlutterView {
//        // 使用FlutterTextureView渲染FlutterView，避免使用FlutterSurfaceView，因为FlutterSurfaceView在应用后台切前台后Flutter的页面会被复用，
//        // 在存在多个FlutterView的情况会出现视图显示错位的问题
//        val flutterTextureView: FlutterTextureView = FlutterTextureView(activity!!)
//        flutterView = FlutterView(context, flutterTextureView)
//        return flutterView!!
//    }
//
//    override fun onStart() {
//        flutterView!!.attachToFlutterEngine(flutterEngine!!)
//        super.onStart()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        flutterEngine!!.lifecycleChannel.appIsResumed()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        flutterEngine!!.lifecycleChannel.appIsInactive()
//    }
//
//    override fun onStop() {
//        super.onStop()
//        flutterEngine!!.lifecycleChannel.appIsPaused()
//    }
//
//    override fun onDetach() {
//        super.onDetach()
//        flutterEngine!!.lifecycleChannel.appIsDetached()
//    }
//}