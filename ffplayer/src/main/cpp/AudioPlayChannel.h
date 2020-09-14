//
// Created by beiying on 2020/9/13.
//

#ifndef LOPNOR_AUDIOPLAYCHANNEL_H
#define LOPNOR_AUDIOPLAYCHANNEL_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "JavaCallHelper.h"
#include "BasePlayChannel.h"

class AudioPlayChannel:public BasePlayChannel {
public:
    AudioPlayChannel(int id, JavaCallHelper *javaCallHelper,
                     AVCodecContext *codecContext);
    ~AudioPlayChannel();

    virtual void startPlay();
    virtual void stopPlay();

    void initOpenSL();

    void decodePcm();

    int getPcm();
    uint8_t *buffer;
private:
    pthread_t pid_audio_play;
    pthread_t pid_audio_decode;
    int out_channels;
    int out_samplesize;
    int out_sample_rate;
};


#endif //LOPNOR_AUDIOPLAYCHANNEL_H
