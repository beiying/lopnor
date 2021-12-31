package com.beiying.media.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import com.beiying.lopnor.base.log.ByLog
import java.io.*

class BYAudioManager private constructor(val context: Context){
    private  var isAudioRecording: Boolean = false
    var mAudioRecord: AudioRecord? = null
    companion object {
        /**
         * 采样率，现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
         */
        val SAMPLE_RATE_INHZ: Int = 44100

        /**
         * 声道数。CHANNEL_IN_MONO and CHANNEL_IN_STEREO. 其中CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
         */
        val CHANNEL_CONFIG: Int = AudioFormat.CHANNEL_IN_MONO

        /**
         * 返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
         */
        val AUDIO_FORMAT: Int = AudioFormat.ENCODING_PCM_16BIT

        const val TAG: String = "BYAudioManager"

        @Volatile private var sInstance: BYAudioManager? = null

        @JvmStatic
        fun getIntance(context: Context): BYAudioManager {
            return sInstance ?: synchronized(this) {
                sInstance ?: BYAudioManager(context).also { sInstance = it }
            }
        }

    }

    suspend fun startRecordAudio(pcmFile: String) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize)
        var data: ByteArray = ByteArray(minBufferSize)

        var file: File = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), pcmFile)
        if (!file.mkdirs()) {
            ByLog.et(TAG, arrayOf("Directory not created"))
        }
        if (file.exists()) {
            file.delete()
        }

        mAudioRecord?.let { audioRecord ->
            audioRecord.startRecording()
            isAudioRecording = true

            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(file)

                fileOutputStream?.let { fos ->
                    while(isAudioRecording) {
                        var readCount = audioRecord.read(data, 0, minBufferSize)
                        if (AudioRecord.ERROR_INVALID_OPERATION != readCount) {
                            fos.write(data)
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                fileOutputStream?.close()
            }
        }
    }

    fun stopRecordAudio() {
        mAudioRecord?.let {audioRecord ->
            audioRecord.stop()
            audioRecord.release()
        }
        mAudioRecord = null
    }

    fun pcmToWav(inFile: String, outFileName: String) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_INHZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        var fis: FileInputStream = FileInputStream(inFile)
        var fos: FileOutputStream = FileOutputStream(outFileName)
        var totalAudioLen = fis.channel.size()
        var totalDataLen: Long = totalAudioLen + 36
        val byteRate: Long = (16 * SAMPLE_RATE_INHZ * CHANNEL_CONFIG / 8).toLong()
        var data: ByteArray = ByteArray(minBufferSize)

        writeWaveFileHeader(fos, totalAudioLen, totalDataLen, SAMPLE_RATE_INHZ, CHANNEL_CONFIG, byteRate)
        while(fis.read(data) != -1) {
            fos.write(data)
        }
        fis.close()
        fos.close()
    }

    /**
     * 加入wav文件头
     */
    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Int, channels: Int, byteRate: Long
    ) {
        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        //WAVE
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        // 4 bytes: size of 'fmt ' chunk
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        // format = 1
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // block align
        header[32] = (2 * 16 / 8).toByte()
        header[33] = 0
        // bits per sample
        header[34] = 16
        header[35] = 0
        //data
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }
}