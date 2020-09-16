package com.beiying.ffplayer

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 提供播放器相应的功能：
 *  1、播放器的准备工作
 * */
class FFPlayer : SurfaceHolder.Callback{

    companion object {
        init {
            System.loadLibrary("avplayer")
        }
    }
    private external fun prepare(dataSource: String)
    private external fun startPlay()
    private external fun setSurface(surface: Any?)

    external fun sound(audioPath: String, outputPath: String)
    private external fun playVideo(videoPath: String, surface: Any?)
    private external fun getVideoDuration(): Int
    private external fun playerSeekTo(progress: Int)
    private external fun stopPlay()
    private external fun releasePlayer()


    private var controllerCallback: IControllerCallback? = null
    private var playerEvent: PlayerEvent? = null
    private var surfaceHolder: SurfaceHolder? = null
    private lateinit var dataSource: String

    fun setDataSource(dataSource: String) {
        this.dataSource = dataSource
    }

    fun setSurfaceView(surfaceView: SurfaceView) {
        this.surfaceHolder?.removeCallback(this)
        this.surfaceHolder = surfaceView.holder
        this.surfaceHolder?.addCallback(this)
    }

    fun setControllerCallback(controllerCallback: IControllerCallback) {
        this.controllerCallback = controllerCallback;
    }

    fun setPlayerEvent(playerEvent: PlayerEvent) {
        this.playerEvent = playerEvent
    }

    fun getDuration(): Int {
        return getVideoDuration()
    }

    fun seekTo(progress: Int) {//拖动比较耗时，在子线程进行
        Thread {
            run {
                playerSeekTo(progress)
            }
        }.start()
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.surfaceHolder = surfaceHolder
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {

    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        setSurface(surfaceHolder.surface)
    }

    fun preparePlayer() {
        prepare(dataSource)
    }

    fun play(videoPath: String) {
        playVideo(videoPath, surfaceHolder?.surface)
    }

    fun start() {
        startPlay()
    }

    fun stop() {
        stopPlay()
    }

    fun release() {
        surfaceHolder?.removeCallback(this)
        releasePlayer()
    }

    fun onPrepared() {//native层准备工作完成后会调用
        controllerCallback?.onPrepare()
    }

    fun onError(errorCode: Int) {
        controllerCallback?.onError(errorCode)
    }

    fun onProgress(progress: Int) {//播放进度
        playerEvent?.onProgress(progress)
    }

    interface IControllerCallback {
        fun onPrepare()
        fun onError(errorCode: Int)
    }

    interface PlayerEvent {

        fun onProgress(progress: Int)

    }
}