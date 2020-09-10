package com.beiying.ffplayer

import android.app.Activity
import android.view.SurfaceHolder

class LivePusher(activity: Activity, width: Int, height:Int, bitrate: Int, fps: Int, cameraId: Int) {
    private var audioChannel: AudioChannel =
        AudioChannel(this,2)
    private var videoChannel: VideoChannel = VideoChannel(this, activity, width, height, bitrate, fps, cameraId)

    init {
        initLivePusher()
    }
    companion object {
        init {
            System.loadLibrary("livepusher")
        }
    }
    external fun initLivePusher()
    external fun setVideoEncInfo(width: Int, height: Int, fps: Int, bitrate: Int)
    external fun startLivePusher(path: String)
    external fun pushVideo(data: ByteArray)
    external fun pushAudio(data: ByteArray)
    external fun setAudioEncInfo(sample: Int, channels: Int)
    external fun getInputSamples(): Int

    fun setPreviewDisplay(surfaceHolder: SurfaceHolder) {
        videoChannel.setPreviewDisplay(surfaceHolder)
    }

    fun switchCamera() {
        videoChannel.switchCamera()
    }

    fun startLive(url: String) {
        startLivePusher(url)
        videoChannel.startLive()
        audioChannel.startLive()
    }

}