package com.beiying.ffplayer

import android.app.Activity
import android.view.SurfaceHolder

class LivePusher(activity: Activity, width: Int, height:Int, bitrate: Int, fps: Int, cameraId: Int) {
    private var audioChannel: AudioChannel =
        AudioChannel(this)
    private var videoChannel: VideoChannel = VideoChannel(this, activity, width, height, bitrate, fps, cameraId)

    init {
        native_init()
    }
    companion object {
        init {
            System.loadLibrary("livepusher")
        }
    }
    external fun native_init()
    external fun native_setVideoEncInfo(width: Int, height: Int, fps: Int, bitrate: Int)
    external fun native_start(path: String)
    external fun native_pushVideo(data: ByteArray)

    fun setPreviewDisplay(surfaceHolder: SurfaceHolder) {
        videoChannel.setPreviewDisplay(surfaceHolder)
    }

    fun switchCamera() {
        videoChannel.switchCamera()
    }

    fun startLive(url: String) {
        native_start(url)
        videoChannel.startLive()
    }
}