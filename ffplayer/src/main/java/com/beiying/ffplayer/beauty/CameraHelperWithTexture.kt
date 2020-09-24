package com.beiying.ffplayer.beauty

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.Surface

class CameraHelperWithTexture(var cameraId: Int) :
    Camera.PreviewCallback {

    private lateinit var camera: Camera
    private lateinit var buffer: ByteArray
    private lateinit var bytes: ByteArray
    private var previewCallback: Camera.PreviewCallback? = null
    private lateinit var surfaceTexture: SurfaceTexture

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
        startPreview()
    }

    /**
     * 开启摄像头
     * */
    fun startPreview() {
        try {
            //获取camera对象
            camera = Camera.open(cameraId)
            val parameters: Camera.Parameters = camera.parameters
            //设置预览数据格式为nv21
            parameters.previewFormat = ImageFormat.NV21
            //设置摄像头宽高
            parameters.setPreviewSize(WIDTH, HEIGHT)

            camera.parameters = parameters
            buffer = ByteArray(WIDTH * HEIGHT * 3 / 2)
            bytes = ByteArray(buffer.size)

            //数据缓存区
            camera.addCallbackBuffer(buffer)
            camera.setPreviewCallbackWithBuffer(this)
            //设置预览画面
            camera.setPreviewTexture(surfaceTexture)
            camera.startPreview()
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * 开启摄像头
     * */
    fun startPreview(surfaceTexture: SurfaceTexture) {
        try {
            //获取camera对象
            camera = Camera.open(cameraId)
            val parameters: Camera.Parameters = camera.parameters
            //设置预览数据格式为nv21
            parameters.previewFormat = ImageFormat.NV21
            //设置摄像头宽高
            parameters.setPreviewSize(WIDTH, HEIGHT)

            camera.parameters = parameters
            buffer = ByteArray(WIDTH * HEIGHT * 3 / 2)
            bytes = ByteArray(buffer.size)

            //数据缓存区
            camera.addCallbackBuffer(buffer)
//            camera.setPreviewCallbackWithBuffer(this)
            //设置预览画面
            camera.setPreviewTexture(surfaceTexture)
            camera.startPreview()
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * 释放摄像头
     * */
    private fun stopPreview() {
        camera?.let {
            it.setPreviewCallback(null)
            it.stopPreview()
            it.release()
        }
    }

    fun setPreviewCallback(previewCallback: Camera.PreviewCallback) {
        this.previewCallback = previewCallback
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera) {
        previewCallback?.onPreviewFrame(bytes, camera)
        camera.addCallbackBuffer(buffer)
    }


}