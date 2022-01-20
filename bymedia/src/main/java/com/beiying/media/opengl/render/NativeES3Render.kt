package com.beiying.media.opengl.render

class NativeES3Render {
    companion object {
        init {
            System.loadLibrary("native-render")
        }
    }


    external fun native_Init()

    external fun native_UnInit()

    external fun native_SetParamsInt(paramType: Int, value0: Int, value1: Int)

    external fun native_SetParamsFloat(paramType: Int, value0: Float, value1: Float)

    external fun native_UpdateTransformMatrix(rotateX: Float, rotateY: Float, scaleX: Float, scaleY: Float)

    external fun native_SetImageData(format: Int, width: Int, height: Int, bytes: ByteArray)

    external fun native_SetImageDataWithIndex(index: Int, format: Int, width: Int, height: Int, bytes: ByteArray)

    external fun native_SetAudioData(audioData: ShortArray)

    external fun native_OnSurfaceCreated()

    external fun native_OnSurfaceChanged(width: Int, height: Int)

    external fun native_OnDrawFrame()
}