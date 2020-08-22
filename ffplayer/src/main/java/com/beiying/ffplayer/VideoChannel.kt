package com.beiying.ffplayer

import android.app.Activity
import android.hardware.Camera

class VideoChannel(
    val livePusher: LivePusher,
    activity: Activity,
    width: Int,
    height: Int,
    val bitrate: Int,
    val fps: Int,
    cameraId: Int
) : Camera.PreviewCallback {
    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        TODO("Not yet implemented")
    }

}