package com.beiying.ffplayer

import android.app.Activity
import android.hardware.Camera
import android.view.SurfaceHolder

class VideoChannel(livePusher: LivePusher, activity: Activity, width: Int, height: Int, val bitrate: Int,
                   val fps: Int, cameraId: Int) : Camera.PreviewCallback, CameraHelper.OnChangedSizeListener {
    private lateinit var cameraHelper: CameraHelper

    init {
        cameraHelper = CameraHelper(activity, cameraId, width, height)
        cameraHelper.setPreviewCallback(this)
        cameraHelper.setOnChangedSizeListener(this)
    }
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
    }

    fun setPreviewDisplay(surfaceHolder: SurfaceHolder) {
        cameraHelper.setPreviewDisplay(surfaceHolder)
    }

    fun switchCamera() {
        cameraHelper.switchCamera()
    }

    override fun onChanged(width: Int, height: Int) {

    }

}