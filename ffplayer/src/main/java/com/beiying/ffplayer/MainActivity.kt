package com.beiying.ffplayer

import android.Manifest
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions3.RxPermissions
import java.io.File

class MainActivity: AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    private lateinit var surfaceView: SurfaceView
    private lateinit var preSurfaceView: SurfaceView
    private lateinit var seekBar: SeekBar
    private lateinit var player: FFPlayer
    private lateinit var livePusher: LivePusher

    var videoProgress: Int = 0
    var isTouch: Boolean = false
    var isSeek: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surfaceView = findViewById(R.id.player_sv)
        preSurfaceView = findViewById(R.id.preview_sv)
        seekBar = findViewById(R.id.player_seekbar)
        seekBar.setOnSeekBarChangeListener(this)


        player = FFPlayer()
        player.setSurfaceView(surfaceView)
        player.setControllerCallback(object: FFPlayer.IControllerCallback {
            override fun onPrepare() {
                player.start()
            }

            override fun onError(errorCode: Int) {
                TODO("Not yet implemented")
            }

        })
        player.setPlayerEvent(object: FFPlayer.PlayerEvent {
            override fun onProgress(progress: Int) {
                if (!isTouch) {
                    runOnUiThread {
                        val duration  = player.getDuration()
                        if (duration != 0) {
                            if (isSeek) {
                                isSeek = false;
                                return@runOnUiThread
                            }
                            seekBar.progress = progress / duration * 100
                        }
                    }
                }
            }

        })
        player.setDataSource( File(Environment.getExternalStorageDirectory(), "1/ffmpeg_test.mp4").absolutePath)

        livePusher = LivePusher(this, 800, 480, 800_000, 10, Camera.CameraInfo.CAMERA_FACING_FRONT)
        livePusher.setPreviewDisplay(preSurfaceView.holder)

        val rxPermissions = RxPermissions(this)
        rxPermissions.requestEach(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

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
//                player.play(file.absolutePath)
                player.preparePlayer()
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

    fun stopVideo(view: View) {

    }

    fun startLive(view: View) {
        livePusher.startLive("rtmp://47.101.155.208/myapp")
    }
    fun stopLive(view: View) {

    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        isTouch = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isTouch = false
        isSeek = true
        videoProgress = player.getDuration() * seekBar.progress / 100
        player.seekTo(videoProgress)
    }


}