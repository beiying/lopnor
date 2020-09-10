//
// Created by dajia on 2020/9/9.
//

#include <cstring>
#include "AudioChannel.h"
#include "librtmp/rtmp.h"

//编码麦克风采集的数据为aac格式，编码后也是封装到RTMPPacket中推送到服务器
void AudioChannel::encodeData(int8_t *data) {
    int byteLen = faacEncEncode(audioCodec, reinterpret_cast<int32_t *>(data), inputSamples, buffer, maxOutputBytes);
    if (byteLen > 0) {
        int bodySize = 2 + byteLen;
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, bodySize);

        //前两个字节都是固定的
        packet->m_body[0] = 0xAF;
        if(channels == 1) {
            packet->m_body[0] = 0xAE;
        }
        packet->m_body[1] = 0x01;

        //之后的是aac数据
        memcpy(&packet->m_body[2], buffer, byteLen);

        //设置Packet参数
        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bodySize;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

        audioCallback(packet);
    }
}

//faac编码器初始化
void AudioChannel::setAudioEncInfo(int sampleInHZ, int channels) {
    //打开faac编码器，获取编码器缓冲大小和最大缓冲区大小，小于麦克风的缓冲区大小
    audioCodec = faacEncOpen(sampleInHZ, channels, &inputSamples, &maxOutputBytes);

    //编码器设置参数
    faacEncConfigurationPtr config = faacEncGetCurrentConfiguration(audioCodec);
    config->mpegVersion = MPEG4;
    config->aacObjectType = LOW;//编码标准，越低编码速度越快
    config->inputFormat = FAAC_INPUT_16BIT;
    config->outputFormat = 0;//编码出原始数据，既不是adts也不是adif
    faacEncSetConfiguration(audioCodec, config);//重新设置编码器参数

    buffer = new u_char[maxOutputBytes];//申请缓冲区
}

int AudioChannel::getInputSamples() {
    return inputSamples;
}


void AudioChannel::setAudioCallback(AudioCallback audioCallback) {
    this->audioCallback = audioCallback;
}
//获取编码器参数，并封装到RTMPPacket推送到服务器，供解码器解码使用
RTMPPacket* AudioChannel::getAudioTag() {
    u_char *buf;
    u_long len;
    faacEncGetDecoderSpecificInfo(audioCodec, &buf, &len);//获取编码器信息
    int bodySize = 2 + len;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, bodySize);

    packet->m_body[0] = 0xAF;
    if (channels == 1) {
        packet->m_body[0] = 0xAE;
    }
    packet->m_body[1] = 0x00;
    memcpy(&packet->m_body[2], buf, len);

    //设置Packet参数
    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    return packet;

}