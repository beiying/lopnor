package com.beiying.media.opengl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLException
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.media.R
import com.beiying.media.opengl.render.NativeEglRender
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer

class EGLActivity: AppCompatActivity() {
    lateinit var mImageView: ImageView
    lateinit var mEglRender: NativeEglRender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egl)
        mImageView = findViewById<ImageView>(R.id.imageView)

        mEglRender = NativeEglRender()
        mEglRender.native_EglRenderInit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_egl, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        var shaderIndex = 0
        when (id) {
            R.id.action_shader0 -> shaderIndex = 0
            R.id.action_shader1 -> shaderIndex = 1
            R.id.action_shader2 -> shaderIndex = 2
            R.id.action_shader3 -> shaderIndex = 3
            R.id.action_shader4 -> shaderIndex = 4
            R.id.action_shader5 -> shaderIndex = 5
            R.id.action_shader6 -> shaderIndex = 6
            else -> {}
        }

        if (mEglRender != null) {
            mEglRender.native_EglRenderSetIntParams(200, shaderIndex)
            startEglRender()
        }
        return true
    }

    fun startEglRender() {
        loadRGBAImage(R.drawable.leg, mEglRender)
        mEglRender.native_EglRenderDraw()
        mImageView.setImageBitmap(createBitmapFromGLSurface(0, 0, 933, 1400))
    }

    private fun loadRGBAImage(resId: Int, render: NativeEglRender) {
        val inputStream = this.resources.openRawResource(resId)
        val bitmap: Bitmap?
        try {
            bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                val bytes = bitmap.byteCount
                val buf = ByteBuffer.allocate(bytes)
                bitmap.copyPixelsToBuffer(buf)
                val byteArray = buf.array()
                render.native_EglRenderSetImageData(byteArray, bitmap.width, bitmap.height)
            }
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int): Bitmap? {
        var bitmapBuffer: IntArray = IntArray(w * h)
        var bitmapSource: IntArray = IntArray(w * h)
        var intBuffer: IntBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)
        try {
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int = 0
            var offset2: Int = 0
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
            return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
        } catch (e: GLException) {
            e.printStackTrace()
        }
        return null
    }

}