//
// Created by dajia on 2020/9/9.
//

#ifndef LOPNOR_AUDIOCHANNEL_H
#define LOPNOR_AUDIOCHANNEL_H


class AudioChannel {

public:
    void encodeData(int8_t *data);
    void setAudioEncInfo(int sampleInHZ, int channels);
private:
    int channels;
    faacEncHandle audioCodec;
    u_long inputSamples;
    u_long maxOutputBytes;
    u_char *buffer = 0;
};


#endif //LOPNOR_AUDIOCHANNEL_H
