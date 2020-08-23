package com.beiying.ffplayer

import android.Manifest
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
    private lateinit var player: FFPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.player_sv)
        player = FFPlayer()
        player.setSurfaceView(surfaceView)

    }

    fun open(view: View) {
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

}