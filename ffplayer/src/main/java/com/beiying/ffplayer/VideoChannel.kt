package com.beiying.ffplayer

import android.app.Activity
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder

class VideoChannel(val livePusher: LivePusher, activity: Activity, width: Int, height: Int, val bitrate: Int,
                   val fps: Int, cameraId: Int) : Camera.PreviewCallback, CameraHelper.OnChangedSizeListener {
    private lateinit var cameraHelper: CameraHelper
    private var isLiving: Boolean = false

    init {
        cameraHelper = CameraHelper(activity, cameraId, width, height)
        cameraHelper.setPreviewCallback(this)
        cameraHelper.setOnChangedSizeListener(this)
    }

    //data是摄像头采集的一帧数据，格式是NV21，推流前需要将其转换为I420
    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        Log.e("liuyu", "VideoChannel onPreviewFrame:${data.size}")
        if (isLiving) {
            livePusher.native_pushVideo(data)
        }
    }

    fun setPreviewDisplay(surfaceHolder: SurfaceHolder) {
        cameraHelper.setPreviewDisplay(surfaceHolder)
    }

    fun switchCamera() {
        cameraHelper.switchCamera()
    }

    override fun onChanged(width: Int, height: Int) {
        livePusher.native_setVideoEncInfo(width, height, fps, bitrate)
    }

    fun startLive() {
        isLiving = true;
    }

}