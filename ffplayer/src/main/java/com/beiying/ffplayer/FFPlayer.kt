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
    external fun sound(audioPath: String, outputPath: String)
    external fun playVideo(videoPath: String, surface: Any?)



    private var surfaceHolder: SurfaceHolder? = null

    fun setSurfaceView(surfaceView: SurfaceView) {
        this.surfaceHolder?.removeCallback(this)
        this.surfaceHolder = surfaceView.holder
        this.surfaceHolder?.addCallback(this)
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.surfaceHolder = surfaceHolder
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {

    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {

    }


    fun play(videoPath: String) {
        playVideo(videoPath, surfaceHolder?.surface)
    }
}