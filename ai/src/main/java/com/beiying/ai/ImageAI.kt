package com.beiying.ai

import android.graphics.Bitmap

class ImageAI {
    companion object {
        init {
            System.loadLibrary("imageutil")
        }
    }

    external fun faceDetectionSaveInfo(bitmap: Bitmap)

    external fun loadCascade(path:String)

    external fun tranformGray(bitmap: Bitmap)
}