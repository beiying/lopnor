package com.beiying.media.opengl

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.beiying.media.R
import com.beiying.media.opengl.render.GLRender
import com.beiying.media.opengl.render.NativeES3Render

class NativeGraphActivity : AppCompatActivity() {
    lateinit var rootView: FrameLayout
    lateinit var nativeSurfaceView: NativeRenderGLSurfaceView
    lateinit var render: GLRender
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_native_graph)

        rootView = findViewById<FrameLayout>(R.id.content_container)

        render = GLRender()
        render.init()

        nativeSurfaceView = NativeRenderGLSurfaceView(this,render)

        rootView.addView(nativeSurfaceView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    }

    override fun onDestroy() {
        super.onDestroy()
        render.unInit()
    }
}