package com.beiying.media.opengl

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.beiying.media.R
import com.beiying.media.opengl.render.GLRender
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_TEXTURE_MAP
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_TRIANGLE
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_YUV_TEXTURE_MAP
import java.io.InputStream
import java.nio.ByteBuffer

class NativeGraphActivity : AppCompatActivity() {
    lateinit var mRootView: FrameLayout
    lateinit var mNativeSurfaceView: NativeRenderGLSurfaceView
    lateinit var mRender: GLRender
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_native_graph)

        mRootView = findViewById<FrameLayout>(R.id.content_container)

        mRender = GLRender()
        mRender.init()

        addSurfaceView()
    }

    private fun addSurfaceView() {
        mNativeSurfaceView = NativeRenderGLSurfaceView(this,mRender)
        mNativeSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        mRootView.addView(mNativeSurfaceView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

    }

    override fun onDestroy() {
        super.onDestroy()
        mRender.unInit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_sample) {
            showGLSampleDialog()
        }
        return true
    }

    fun showGLSampleDialog() {
        val builder = AlertDialog.Builder(this)
        val dialog: Dialog = builder.setTitle("").setItems(items,
            DialogInterface.OnClickListener { dialog, position ->
                dialog.dismiss()
                mRootView.removeView(mNativeSurfaceView)
                addSurfaceView()

                mRender.setParamsInt(GLRender.SAMPLE_TYPE, position + GLRender.SAMPLE_TYPE, 0)
                when(position + GLRender.SAMPLE_TYPE) {
                    SAMPLE_TYPE_TRIANGLE -> {}
                    SAMPLE_TYPE_TEXTURE_MAP -> {loadRGBAImage(R.drawable.texture_map)}
                    SAMPLE_TYPE_YUV_TEXTURE_MAP -> {loadNV21Image()}
                }
            }).create()
        dialog.show()
    }

    /**
     * 2、加载RGBA格式的图片实现纹理渲染
     * */
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
                mRender.setImageData(NativeRenderGLSurfaceView.IMAGE_FORMAT_RGBA, bmp.width, bmp.height, byteArray)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return bitmap
    }

    private fun loadNV21Image() {
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open("YUV_Image_840x1074.NV21")
            var length: Int = inputStream.available()
            var buffer: ByteArray = ByteArray(length)
            inputStream.read(buffer)
            mRender.setImageData(NativeRenderGLSurfaceView.IMAGE_FORMAT_NV21, 840, 1074, buffer)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            inputStream?.let {
                it.close()
            }
        }
    }

    private val items: Array<String> = arrayOf(
        "DrawTriangle",
        "TextureMap",
        "YUV Rendering",
        "VAO&VBO",
        "FBO Offscreen Rendering",
        "EGL Background Rendering",
        "FBO Stretching",
        "Coordinate System",
        "Basic Lighting",
        "Transform Feedback",
        "Complex Lighting",
        "Depth Testing",
        "Instancing",
        "Stencil Testing",
        "Blending",
        "Particles",
        "SkyBox",
        "Assimp Load 3D Model",
        "PBO",
        "Beating Heart",
        "Cloud",
        "Time Tunnel",
        "Bezier Curve",
        "Big Eyes",
        "Face Slender",
        "Big Head",
        "Rotary Head",
        "Visualize Audio",
        "Scratch Card",
        "3D Avatar",
        "Shock Wave",
        "MRT",
        "FBO Blit",
        "Texture Buffer",
        "Uniform Buffer",
        "RGB to YUYV",
        "Multi-Thread Render",
        "Text Render",
        "Portrait stay color",
        "GL Transitions_1",
        "GL Transitions_2",
        "GL Transitions_3",
        "GL Transitions_4",
        "RGB to NV21",
        "RGB to I420"
    )
}