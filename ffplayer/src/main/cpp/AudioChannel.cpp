//
// Created by dajia on 2020/9/9.
//

#include "AudioChannel.h"

void AudioChannel::encodeData(int8_t *data) {

}

void AudioChannel::setAudioEncInfo(int sampleInHZ, int channels) {
    faacEncOpen(sampleInHZ, channels,   )
}