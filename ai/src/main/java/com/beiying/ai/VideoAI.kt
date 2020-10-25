package com.beiying.ai

import android.view.Surface

class VideoAI {
    companion object {
        init {
            System.loadLibrary("videoutil")
        }
    }

    external fun initCascade(model: String)

    external fun setSurface(surface: Surface)

    external fun postData(data: ByteArray, w: Int, h: Int, cameraId: Int)

    external fun releaseCascade()
}