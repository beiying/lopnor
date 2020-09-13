//
// Created by beiying on 2020/9/13.
//

#ifndef LOPNOR_AUDIOPLAYCHANNEL_H
#define LOPNOR_AUDIOPLAYCHANNEL_H

#include "JavaCallHelper.h"
#include "BasePlayChannel.h"

class AudioPlayChannel:public BasePlayChannel {
public:
    AudioPlayChannel(int id, JavaCallHelper *javaCallHelper,
                     AVCodecContext *codecContext);
    ~AudioPlayChannel();

    virtual void startPlay();
    virtual void stopPlay();
};


#endif //LOPNOR_AUDIOPLAYCHANNEL_H
