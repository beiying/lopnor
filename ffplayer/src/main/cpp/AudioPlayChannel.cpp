//
// Created by beiying on 2020/9/13.
//

#include "AudioPlayChannel.h"

AudioPlayChannel::AudioPlayChannel(int id, JavaCallHelper *javaCallHelper,
                                   AVCodecContext *codecContext) : BasePlayChannel(id, javaCallHelper, codecContext) {

}

AudioPlayChannel::~AudioPlayChannel() {

}

void AudioPlayChannel::startPlay() {

}

void AudioPlayChannel::stopPlay() {

}
