package com.beiying.media.opengl.render

class NativeES3Render {
    companion object {
        init {
            System.loadLibrary("native-render")
        }
        val SAMPLE_TYPE = 200

        val SAMPLE_TYPE_TRIANGLE = SAMPLE_TYPE
        val SAMPLE_TYPE_TEXTURE_MAP = SAMPLE_TYPE + 1
        val SAMPLE_TYPE_YUV_TEXTURE_MAP = SAMPLE_TYPE + 2
        val SAMPLE_TYPE_VAO = SAMPLE_TYPE + 3
        val SAMPLE_TYPE_FBO = SAMPLE_TYPE + 4
        val SAMPLE_TYPE_EGL = SAMPLE_TYPE + 5
        val SAMPLE_TYPE_FBO_LEG = SAMPLE_TYPE + 6
        val SAMPLE_TYPE_COORD_SYSTEM = SAMPLE_TYPE + 7
        val SAMPLE_TYPE_BASIC_LIGHTING = SAMPLE_TYPE + 8
        val SAMPLE_TYPE_TRANS_FEEDBACK = SAMPLE_TYPE + 9
        val SAMPLE_TYPE_MULTI_LIGHTS = SAMPLE_TYPE + 10
        val SAMPLE_TYPE_DEPTH_TESTING = SAMPLE_TYPE + 11
        val SAMPLE_TYPE_INSTANCING = SAMPLE_TYPE + 12
        val SAMPLE_TYPE_STENCIL_TESTING = SAMPLE_TYPE + 13
        val SAMPLE_TYPE_BLENDING = SAMPLE_TYPE + 14
        val SAMPLE_TYPE_PARTICLES = SAMPLE_TYPE + 15
        val SAMPLE_TYPE_SKYBOX = SAMPLE_TYPE + 16
        val SAMPLE_TYPE_3D_MODEL = SAMPLE_TYPE + 17
        val SAMPLE_TYPE_PBO = SAMPLE_TYPE + 18
        val SAMPLE_TYPE_KEY_BEATING_HEART = SAMPLE_TYPE + 19
        val SAMPLE_TYPE_KEY_CLOUD = SAMPLE_TYPE + 20
        val SAMPLE_TYPE_KEY_TIME_TUNNEL = SAMPLE_TYPE + 21
        val SAMPLE_TYPE_KEY_BEZIER_CURVE = SAMPLE_TYPE + 22
        val SAMPLE_TYPE_KEY_BIG_EYES = SAMPLE_TYPE + 23
        val SAMPLE_TYPE_KEY_FACE_SLENDER = SAMPLE_TYPE + 24
        val SAMPLE_TYPE_KEY_BIG_HEAD = SAMPLE_TYPE + 25
        val SAMPLE_TYPE_KEY_ROTARY_HEAD = SAMPLE_TYPE + 26
        val SAMPLE_TYPE_KEY_VISUALIZE_AUDIO = SAMPLE_TYPE + 27
        val SAMPLE_TYPE_KEY_SCRATCH_CARD = SAMPLE_TYPE + 28
        val SAMPLE_TYPE_KEY_AVATAR = SAMPLE_TYPE + 29
        val SAMPLE_TYPE_KEY_SHOCK_WAVE = SAMPLE_TYPE + 30
        val SAMPLE_TYPE_KEY_MRT = SAMPLE_TYPE + 31
        val SAMPLE_TYPE_KEY_FBO_BLIT = SAMPLE_TYPE + 32
        val SAMPLE_TYPE_KEY_TBO = SAMPLE_TYPE + 33
        val SAMPLE_TYPE_KEY_UBO = SAMPLE_TYPE + 34
        val SAMPLE_TYPE_KEY_RGB2YUYV = SAMPLE_TYPE + 35
        val SAMPLE_TYPE_KEY_MULTI_THREAD_RENDER = SAMPLE_TYPE + 36
        val SAMPLE_TYPE_KEY_TEXT_RENDER = SAMPLE_TYPE + 37
        val SAMPLE_TYPE_KEY_STAY_COLOR = SAMPLE_TYPE + 38
        val SAMPLE_TYPE_KEY_TRANSITIONS_1 = SAMPLE_TYPE + 39
        val SAMPLE_TYPE_KEY_TRANSITIONS_2 = SAMPLE_TYPE + 40
        val SAMPLE_TYPE_KEY_TRANSITIONS_3 = SAMPLE_TYPE + 41
        val SAMPLE_TYPE_KEY_TRANSITIONS_4 = SAMPLE_TYPE + 42
        val SAMPLE_TYPE_KEY_RGB2NV21 = SAMPLE_TYPE + 43
        val SAMPLE_TYPE_KEY_RGB2I420 = SAMPLE_TYPE + 44
        val SAMPLE_TYPE_KEY_RGB2I444 = SAMPLE_TYPE + 45
        val SAMPLE_TYPE_KEY_HWBuffer = SAMPLE_TYPE + 46

        val SAMPLE_TYPE_SET_TOUCH_LOC = SAMPLE_TYPE + 999
        val SAMPLE_TYPE_SET_GRAVITY_XY = SAMPLE_TYPE + 1000
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