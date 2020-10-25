package com.beiying.ffplayer

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import java.lang.Exception
import kotlin.math.abs

class CameraHelper(val activity: Activity, var cameraId: Int, var width: Int, var height: Int) :
    SurfaceHolder.Callback, Camera.PreviewCallback {

    private lateinit var camera: Camera
    private lateinit var buffer: ByteArray
    private lateinit var bytes: ByteArray
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var previewCallback: Camera.PreviewCallback
    private var rotation: Int = 0
    private lateinit var onChangedSizeListener: OnChangedSizeListener
    private lateinit var surfaceTexture: SurfaceTexture
    private var shouldRotate: Boolean = true

    companion object {
        val WIDTH: Int = 640
        val HEIGHT: Int = 480
    }
    fun switchCamera() {
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT
        } else {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK
        }
        stopPreview()
        startPreView()
    }

    /**
     * 开启摄像头
     * */
    private fun startPreView() {
        try {
            //获取camera对象
            camera = Camera.open(cameraId)
            val parameters: Camera.Parameters = camera.parameters
            //设置预览数据格式为nv21
            parameters.previewFormat = ImageFormat.NV21
            //设置摄像头宽高
//            parameters.setPreviewSize(WIDTH, HEIGHT)
            setPreviewSize(parameters)
            //设置预览图像角度和方向，并没有改变摄像头采集图像的角度和方向，编码时还需要将采集的数据进行旋转
            setPreviewOrientation(parameters)

            camera.parameters = parameters
            buffer = ByteArray(width * height * 3 / 2)
            bytes = ByteArray(buffer.size)

            //数据缓存区
            camera.addCallbackBuffer(buffer)
            camera.setPreviewCallbackWithBuffer(this)
            //设置预览画面
            //设置预览画面
            camera.setPreviewDisplay(surfaceHolder)
            camera.startPreview()
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    fun startBackgroundPreview() {
        try {
            //交给native层处理旋转
            shouldRotate = false
            //获取camera对象
            camera = Camera.open(cameraId)
            val parameters: Camera.Parameters = camera.parameters
            //设置预览数据格式为nv21
            parameters.previewFormat = ImageFormat.NV21
            //设置摄像头宽高
            parameters.setPreviewSize(WIDTH, HEIGHT)
            camera.parameters = parameters

            buffer = ByteArray(WIDTH * HEIGHT * 3 / 2)
            //数据缓存区
            camera.addCallbackBuffer(buffer)
            camera.setPreviewCallbackWithBuffer(this)
            //设置预览画面
            surfaceTexture = SurfaceTexture(11)
            camera.setPreviewTexture(surfaceTexture)
            camera.startPreview()
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * 释放摄像头
     * */
    fun stopPreview() {
        camera?.let {
            it.setPreviewCallback(null)
            it.stopPreview()
            it.release()
        }
    }

    fun setPreviewDisplay(surfaceHolder: SurfaceHolder) {
        this.surfaceHolder = surfaceHolder
        this.surfaceHolder.addCallback(this)
    }

    fun setPreviewCallback(previewCallback: Camera.PreviewCallback) {
        this.previewCallback = previewCallback
    }

    // 根据摄像头支持的宽高，找出接近期望宽高的尺寸，并设置为该尺寸
    private fun setPreviewSize(parameters: Camera.Parameters) {
        //获取摄像头支持的宽高
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        var size: Camera.Size = supportedPreviewSizes[0]
        Log.e("liuyu", "支持${size.width} * ${size.height}")

        var m: Int = abs(size.height * size.width - width * height)
        supportedPreviewSizes.removeAt(0)

        //从摄像头支持的宽高中找出最接近期望宽高的尺寸
        for (item in supportedPreviewSizes) {
            Log.e("liuyu", "支持${item.width} * ${item.height}")
            val n: Int = abs(item.height * item.width - width * height)
            if (n < m) {
                m = n
                size = item
            }
        }
        this.width = size.width
        this.height = size.height
        parameters.setPreviewSize(this.width, this.height)
        Log.e("liuyu", "设置预览分辨率 width:${width}, height=${height}")
    }

    private fun setPreviewOrientation(parameters: Camera.Parameters) {
        val info: Camera.CameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        rotation = activity.windowManager.defaultDisplay.rotation

        var degrees: Int = 0
        when(rotation) {
            Surface.ROTATION_0 -> {
                degrees = 0
                onChangedSizeListener.onChanged(width, height)
            }
            Surface.ROTATION_90 -> {//横屏，左边是头部（home键在右边）
                degrees = 90
                onChangedSizeListener.onChanged(width, height)
            }
            Surface.ROTATION_270 -> {//横屏，头部在右边
                degrees = 270
                onChangedSizeListener.onChanged(width, height)
            }
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.e("liuyu", "CameraHelper surfaceChanged")
        stopPreview()
        startPreView()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        Log.e("liuyu", "CameraHelper surfaceDestroyed")
        stopPreview()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.e("liuyu", "CameraHelper surfaceCreated")
        startPreView()
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        if(shouldRotate) {
            when(rotation) {
                Surface.ROTATION_0 -> {
                    rotation90(data)
                }
                Surface.ROTATION_90 -> {//横屏，左边是头部（home键在右边）

                }
                Surface.ROTATION_270 -> {//横屏，头部在右边

                }
            }
        }
        previewCallback.onPreviewFrame(data, camera)
        camera.addCallbackBuffer(buffer)
    }

    private fun rotation90(data: ByteArray) {
        var index: Int = 0
        val ySize: Int = width * height
        val uvHeight: Int = height / 2
        //后置摄像头顺时针旋转90度
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            for (i in 0 until width) {
                for(j in (height - 1)..0) {
                    data[index++] = data[width * j + i]
                }
            }
            for (i in 0 until width step 2) {
                for(j in (height - 1)..0) {
                    //u
                    data[index++] = data[ySize + width * j + i]
                    //v
                    data[index++] = data[ySize + width * j + i + 1]
                }
            }
        } else {
            for (i in 0 until width) {
                var mPos: Int = width - 1
                for(j in 0 until height) {
                    data[index++] = data[width * j + i]
                    mPos += width
                }
            }

            for (i in 0 until width step 2) {
                var mPos: Int = ySize + width - 1
                for(j in 0 until height) {
                    data[index++] = data[mPos - i - 1]
                    data[index++] = data[mPos - i]
                    mPos += width
                }
            }
        }
    }

    fun setOnChangedSizeListener(listener: OnChangedSizeListener) {
        this.onChangedSizeListener = listener
    }

    interface OnChangedSizeListener {
        fun onChanged(width: Int, height: Int)
    }

}