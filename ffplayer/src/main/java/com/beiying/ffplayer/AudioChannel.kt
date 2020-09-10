package com.beiying.ffplayer

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AudioChannel(val livePusher: LivePusher, channels: Int) {
    lateinit var audioRecord: AudioRecord
    var channelConfig = AudioFormat.CHANNEL_IN_STEREO
    var executorService: ExecutorService = Executors.newSingleThreadExecutor()
    var isLiving: Boolean = false
    var minBufferSize: Int = 0
    var inputSamples: Int = 0

    init {
        channelConfig = if (channels == 2) {
            AudioFormat.CHANNEL_IN_STEREO
        } else {
            AudioFormat.CHANNEL_IN_MONO
        }
        inputSamples = livePusher.getInputSamples() * 2
        livePusher.setAudioEncInfo(44100, channels)

        minBufferSize = AudioRecord.getMinBufferSize(
            44100,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2  //双通道，16位是两个字节，所以需要乘以2
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            if (minBufferSize > inputSamples) inputSamples else minBufferSize
        )
    }

    fun startLive() {
        isLiving = true
        executorService.submit(AudioTask())
    }

    inner class AudioTask : Runnable {
        override fun run() {
            audioRecord.startRecording()
            var bytes: ByteArray = ByteArray(minBufferSize)
            while (isLiving) {
                var len: Int = audioRecord.read(bytes, 0, bytes.size)
                livePusher.pushAudio(bytes)
            }
        }

    }
}