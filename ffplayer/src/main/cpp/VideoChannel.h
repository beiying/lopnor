//
// Created by beiying on 2020/8/17.
//

#ifndef LOPNOR_VIDEOCHANNEL_H
#define LOPNOR_VIDEOCHANNEL_H

#include <stdint.h>
#include <inttypes.h>
#include <libsx264/x264.h>
#include <jni.h>

//负责解码摄像头采集的视频流
class VideoChannel {
public:
    void setVideoEncInfo(int width, int height, int fps, int bitrate);

    void encodeData(int8_t *data);

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
    x264_t *videoCodec;//x264编码器
    x264_picture_t  *pic_in;//代表一帧，用于存储编码后的数据
    void sendSpsPps(uint8_t sps[100], uint8_t pps[100], int sps_len, int pps_len);
};


#endif //LOPNOR_VIDEOCHANNEL_H
