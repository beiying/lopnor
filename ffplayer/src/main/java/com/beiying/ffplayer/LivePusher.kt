package com.beiying.ffplayer

import android.app.Activity
import android.view.SurfaceHolder

class LivePusher(activity: Activity, width: Int, height:Int, bitrate: Int, fps: Int, cameraId: Int) {
    private var audioChannel: AudioChannel =
        AudioChannel(this)
    private var videoChannel: VideoChannel = VideoChannel(this, activity, width, height, bitrate, fps, cameraId)

    fun setPreviewDisplay(surfaceHolder: SurfaceHolder) {
        videoChannel.setPreviewDisplay(surfaceHolder)
    }

    fun switchCamera() {
        videoChannel.switchCamera()
    }
}