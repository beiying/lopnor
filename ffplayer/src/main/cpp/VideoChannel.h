//
// Created by beiying on 2020/8/17.
//

#ifndef LOPNOR_VIDEOCHANNEL_H
#define LOPNOR_VIDEOCHANNEL_H

class VideoChannel {
public:
    void setVideoEncInfo(int width, int height, int fps, int bitrate);

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
};


#endif //LOPNOR_VIDEOCHANNEL_H
