package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.beiying.media.opengl.render.GLRender

class NativeRenderGLSurfaceView(context: Context, val render: GLRender, attributeset: AttributeSet?): GLSurfaceView(context, attributeset) {
    constructor(context: Context, render: GLRender): this(context, render,null)

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 8)
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY

    }

    companion object {
        const val IMAGE_FORMAT_RGBA = 0x01
        const val IMAGE_FORMAT_NV21 = 0x02
        const val IMAGE_FORMAT_NV12 = 0x03
        const val IMAGE_FORMAT_I420 = 0x04
        const val IMAGE_FORMAT_YUYV = 0x05
        const val IMAGE_FORMAT_GARY = 0x06
    }

}