package com.beiying.media.opengl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.beiying.media.R
import com.beiying.media.opengl.render.GLRender
import com.beiying.media.opengl.render.NativeES3Render
import java.io.InputStream
import java.nio.ByteBuffer

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

        render.setParamsInt(GLRender.SAMPLE_TYPE, GLRender.SAMPLE_TYPE_TEXTURE_MAP, 0)
        loadRGBAImage(R.drawable.texture_map)
    }

    override fun onDestroy() {
        super.onDestroy()
        render.unInit()
    }

    private fun loadRGBAImage(resId: Int): Bitmap? {
        val inputStream: InputStream = this.resources.openRawResource(resId)
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeStream(inputStream);
            bitmap?.let { bmp ->
                var bytesCount = bmp.byteCount
                var buf: ByteBuffer = ByteBuffer.allocate(bmp.byteCount);
                bmp.copyPixelsToBuffer(buf)
                var byteArray: ByteArray = buf.array()
                render.setImageData(NativeRenderGLSurfaceView.IMAGE_FORMAT_RGBA, bmp.width, bmp.height, byteArray)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return bitmap
    }
}