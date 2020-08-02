package com.beiying.ffplayer

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
        Log.e("liuyu", Environment.getDataDirectory().absolutePath)
        val file: File = File(Environment.getExternalStorageDirectory(), "")
        player.play(file.absolutePath)
    }

}