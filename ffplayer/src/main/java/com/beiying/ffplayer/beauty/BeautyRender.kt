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
    private var matrix: FloatArray = FloatArray(16)

    private lateinit var cameraFilter: CameraFilter
    private lateinit var screenFilter: ScreenFilter

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        //打开摄像头
        cameraHelper = CameraHelperWithTexture(Camera.CameraInfo.CAMERA_FACING_BACK)
        //摄像头的图像数据通过纹理传给openGL，即摄像头纹理
        textures = IntArray(1)
        GLES20.glGenTextures(textures.size, textures, 0)//创建纹理
        surfaceTexture = SurfaceTexture(textures[0])
        surfaceTexture.setOnFrameAvailableListener(this)
        //获取摄像头的纹理矩阵，使摄像头采集的图像数据不会变形，即通过该矩阵来转换纹理坐标
        surfaceTexture.getTransformMatrix(matrix)

        cameraFilter = CameraFilter(beautyView.context)
        screenFilter = ScreenFilter(beautyView.context)
        cameraFilter.matrix = matrix
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        cameraHelper.startPreview(surfaceTexture)
        cameraFilter.onSurfaceReady(width, height)
        screenFilter.onSurfaceReady(width, height)
    }

    //每当摄像头生成一帧数据时,即相机有新的预览数据时，请求OpenGL绘制，会回调Render的onDrawFrame方法
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        beautyView.requestRender()
    }

    //摄像头生成一帧数据数据时并需要渲染时会回调该方法
    override fun onDrawFrame(gl: GL10) {
        //重置GPU中的颜色缓冲区的色值
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        //将接收的预览数据更新到SurfaceTexture所关联的OpenGL的OES类型（不同于texture2D）的纹理中
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(matrix)
        cameraFilter.matrix = matrix

        //生成第一个滤镜特效处理后的新的纹理id
        val id: Int = cameraFilter.onDrawFrame(textures[0])

        //最后一个显示特效
        screenFilter.onDrawFrame(id)



    }
}