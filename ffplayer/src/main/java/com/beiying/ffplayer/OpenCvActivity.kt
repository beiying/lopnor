package com.beiying.ffplayer

import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.beiying.ai.VideoAI

class OpenCvActivity : AppCompatActivity(), Camera.PreviewCallback {
    private lateinit var cameraPreview: SurfaceView
    private lateinit var cameraHelper: CameraHelper
    private lateinit var videoAI: VideoAI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opencv)

        cameraPreview = findViewById(R.id.camera_preview)
        cameraHelper = CameraHelper(this,Camera.CameraInfo.CAMERA_FACING_FRONT, 640, 480)
        cameraHelper.setPreviewCallback(this)

        videoAI = VideoAI();

    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        videoAI.initCascade("/sdcard/lbpcascade_frontalface.xml")
        cameraHelper.startBackgroundPreview()
    }
}