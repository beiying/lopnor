package com.beiying.ffplayer

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class FFPlayer : SurfaceHolder.Callback{

    companion object {
        init {
            System.loadLibrary("avplayer")
        }
    }

    external fun playVideo(videoPath: String, surface: Surface)
    external fun sound(audioPath: String, outputPath: String)

    private lateinit var surfaceHolder: SurfaceHolder

    fun setSurfaceView(surfaceView: SurfaceView) {
        if (surfaceHolder != null) {
            this.surfaceHolder.removeCallback(this)
        }
        this.surfaceHolder = surfaceView.holder
        this.surfaceHolder.addCallback(this)
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.surfaceHolder = surfaceHolder
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {

    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {

    }


    fun play(videoPath: String) {
        playVideo(videoPath, surfaceHolder.surface)
    }
}