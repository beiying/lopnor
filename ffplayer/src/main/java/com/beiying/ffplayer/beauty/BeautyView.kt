package com.beiying.ffplayer.beauty

import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class BeautyView(context: Context, attributeSet: AttributeSet?) : GLSurfaceView(context, attributeSet) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(BeautyRender(context as Activity,this))

        //设置按需渲染，当我们调用requestRender请求，GLThread回调一次onDrawFrame；连续渲染就是自动回调onDrawFrame
        renderMode = RENDERMODE_WHEN_DIRTY
    }
    constructor(context: Context): this(context, null) {

    }
}