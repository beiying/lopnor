package com.beiying.ffplayer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Surface;
import android.view.SurfaceView;

public class VideoView extends SurfaceView {
    static {
//        System.loadLibrary("avplayer");
    }

    Surface mSurface;
    public VideoView(Context context) {
        super(context);
        init();
    }

    private void init() {
        getHolder().setFormat(PixelFormat.RGBA_8888);
        mSurface = getHolder().getSurface();
    }

    public void playVideo(final String videoPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                playVideo(videoPath, mSurface);
            }
        }).start();
    }

//    public static native String urlProtocolInfo();
//
//    public static native String avFormatInfo();
//
//    public static native String avCodecInfo();
//
//    public static native String avFilterInfo();
//
//    public static native void playVideo(String videoPath, Surface surface);
}
