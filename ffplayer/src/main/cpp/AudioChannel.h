//
// Created by dajia on 2020/9/9.
//

#ifndef LOPNOR_AUDIOCHANNEL_H
#define LOPNOR_AUDIOCHANNEL_H


#include <cstdint>
#include <sys/types.h>
#include "libfaac/faac.h"
#include "librtmp/rtmp.h"

class AudioChannel {
    typedef void (*AudioCallback)(RTMPPacket *packet);
public:
    void encodeData(int8_t *data);
    void setAudioEncInfo(int sampleInHZ, int channels);
    int getInputSamples();
    void setAudioCallback(AudioCallback audioCallback);
private:
    int channels;
    faacEncHandle audioCodec;
    u_long inputSamples;
    u_long maxOutputBytes;
    u_char *buffer = 0;
    AudioCallback audioCallback;
};


#endif //LOPNOR_AUDIOCHANNEL_H
