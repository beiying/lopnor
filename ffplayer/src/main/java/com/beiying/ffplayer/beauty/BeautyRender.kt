package com.beiying.ffplayer.beauty

import android.app.Activity
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BeautyRender(var activity: Activity, val beautyView: BeautyView): GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {
    private lateinit var cameraHelper: CameraHelperWithTexture
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit  var textures: IntArray
    override fun onDrawFrame(gl: GL10) {

    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        //打开摄像头
        cameraHelper = CameraHelperWithTexture(Camera.CameraInfo.CAMERA_FACING_BACK)
        //摄像头的图像数据通过纹理传给openGL
        textures = IntArray(1)
        GLES20.glGenTextures(textures.size, textures, 0)//创建纹理
        surfaceTexture = SurfaceTexture(textures[0])
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {

        beautyView.requestRender()
    }

}