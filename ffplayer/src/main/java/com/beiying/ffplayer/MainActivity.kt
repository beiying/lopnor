package com.beiying.ffplayer

import android.Manifest
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions3.RxPermissions
import java.io.File

class MainActivity: AppCompatActivity() {
    private lateinit var surfaceView: SurfaceView
    private lateinit var preSurfaceView: SurfaceView
    private lateinit var player: FFPlayer
    private lateinit var livePusher: LivePusher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.player_sv)
        preSurfaceView = findViewById(R.id.preview_sv)

        player = FFPlayer()
        player.setSurfaceView(surfaceView)

        livePusher = LivePusher(this, 800, 480, 800_000, 10, Camera.CameraInfo.CAMERA_FACING_FRONT)
        livePusher.setPreviewDisplay(preSurfaceView.holder)

    }

    fun openVideo(view: View) {
        val rxPermissions = RxPermissions(this)
        rxPermissions.requestEach(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).subscribe {
            if (it.granted) {
                val file: File = File(Environment.getExternalStorageDirectory(), "1/ffmpeg_test.mp4")
                Log.e("liuyu", file.absolutePath)
                player.play(file.absolutePath)
            }
        }
    }

    fun openAudio(view: View) {
        val rxPermissions = RxPermissions(this)
        rxPermissions.requestEach(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).subscribe {
            if (it.granted) {
                val file: File = File(Environment.getExternalStorageDirectory(), "1/test.mp3")
                Log.e("liuyu", file.absolutePath)
                player.sound(file.absolutePath, File(Environment.getExternalStorageDirectory(), "1/out.pcm").absolutePath)
            }
        }
    }

    fun startLive(view: View) {
        livePusher.startLive("rtmp://47.101.155.208/myapp")
    }
    fun stopLive(view: View) {}

}