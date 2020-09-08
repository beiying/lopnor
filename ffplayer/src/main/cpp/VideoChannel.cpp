//
// Created by beiying on 2020/8/17.
//

#include <cstring>
#include "VideoChannel.h"
#include "../../../libs/includes/libsx264/x264.h"
#include "librtmp/rtmp.h"
#include "logger.h"

void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;
    ySize = width * height;
    uvSize = ySize / 4;

    //开始初始化x264编码器

    x264_param_t param;//设置x264编码器参数
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    //编码复杂度
    param.i_level_idc = 32;
    //推流的数据格式为I420，Android平台需要把摄像头采集的NV21数据转换为I420
    param.i_csp = X264_CSP_I420;

    param.i_width = width;
    param.i_height = height;

    param.i_bframe = 0;//首开无B帧

    param.rc.i_rc_method = X264_RC_ABR;//参数i_rc_method表示码率控制，CQP是恒定质量、CRF是恒定码率、ABR是平均码率
    param.rc.i_bitrate = bitrate / 1000;//码率（比特率，单位kbps）
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2;//瞬时最大码率
    param.rc.i_vbv_buffer_size = bitrate /1000;//设置i_vbv_max_bitrate参数就必须设置该参数，码率空值区大小，单位kbps

    param.i_fps_num = fps;
    param.i_fps_den = 1;//帧率的分母，表示1秒钟的帧率

    //时间基的分母和分子，用于计算一帧多少秒，每隔多长时间是一帧
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;

    param.b_vfr_input = 0;//vfr输入，1表示时间基和时间戳用于码率空值，0表示仅帧率用于码率，即用fps而不是时间戳来计算帧间距离
    param.i_keyint_max = fps * 2;//帧距离（关键帧）2s一个关键帧
    param.b_repeat_headers = 1;//是否赋值sps和pps放在每个关键帧的前面，该参数设置是让每个关键帧都附带sps和pps
    param.i_threads = 1;//用于设置并行编码多帧，表示线程数，0表示自动多线程编码

    x264_param_apply_profile(&param, "baseline");//设置编码的图像质量

    videoCodec = x264_encoder_open(&param);//打开x264编码器

    pic_in = new x264_picture_t();//创建一帧数据容器
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);

}

void VideoChannel::setVideoCallback(VideoCallback videoCallback) {
    this->videoCallback = videoCallback;
}

void VideoChannel::encodeData(int8_t *data) {
    //将NV21数据转换为YUV_I420，Y存放的位置相同，UV的存放位置不同

    //pic_in->img.plane是个指针数组，用于存放YUV数据，第0个存放Y，第1个存放U，第2个存放V
    memcpy(pic_in->img.plane[0], data, ySize);
    for (int i = 0; i < uvSize;++i) {
        //NV21中按VUV方式交错存放，U数据位于奇数位，V数据位于偶数位
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);//提取U
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);//提取V
    }

    x264_nal_t *pp_nal;//编码后产生的NALU单元

    int pi_nal;//一帧图片编码出来的数据中有多少个NALU单元
    x264_picture_t pic_out;
    x264_encoder_encode(videoCodec,&pp_nal, &pi_nal , pic_in, &pic_out);
    //编码后的H264数据中，关键帧一般都是以00 00 00 01开头，后面紧接着就是SPS、PPS数据
    int sps_len;
    int pps_len;
    uint8_t sps[100];
    uint8_t pps[100];

    //对于H.264而言一帧图像每个NALU单元的界定符为00 00 00 01或者00 00 01。
    for (int i = 0; i < pi_nal;++i) {//如果当前编码的数据是关键帧数据，解码出的NALU单元中就有SPS和PPS
        if (pp_nal[i].i_type == NAL_SPS) {
            //对于H264而言，SPS就是编码后的第一个NALU单元。如果是读取H264文件，就是第一个NALU单元界定符与第二个NALU单元界定符中间的数据长度是4。
            sps_len = pp_nal->i_payload - 4;//减去00 00 00 01四个字节
            memcpy(sps, pp_nal[i].p_payload + 4, sps_len);
        } else if (pp_nal[i].i_type == NAL_PPS) {
            //对于H264而言，PPS就是编码后的第二个NALU单元。如果是读取H264文件，就是第二个NALU单元界定符与第三个NALU单元界定符中间的数据，长度不固定。
            pps_len = pp_nal->i_payload - 4;//减去00 00 00 01四个字节
            memcpy(pps, pp_nal[i].p_payload + 4, pps_len);
            sendSpsPps(sps, pps, sps_len, pps_len);//关键帧需要将SPS和PPS封装到RMTPPacket中一起发送出去，以便于播放直播画面时解码器解码
        } else {
            sendFrame(pp_nal[i].i_type, pp_nal[i].p_payload, pp_nal[i].i_payload);
        }
    }
}

//将SPS和PPS按照RTMP协议规定的格式封装在一起推送到服务器
void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    int bodySize = 5 + 8 + sps_len + 3 + pps_len;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, bodySize);

    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //X264版本
    packet->m_body[i++] = 0x01;

    //Profile，对应SPS[1]
    packet->m_body[i++] = sps[1];
    //兼容性, 对应SPS[2]
    packet->m_body[i++] = sps[2];
    //Profile Level， 对应SPS[3]
    packet->m_body[i++] = sps[3];

    //包长数据所使用的字节数，通常为0xFF
    packet->m_body[i++] = 0xFF;
    //SPS个数，通常为0xE1
    packet->m_body[i++] = 0xE1;

    //sps长度，两个字节表示，需要将一个字节的int类型转换成两个字节
    packet->m_body[i++] = (sps_len >> 8);//高8位
    packet->m_body[i++] = sps_len & 0xFF;//低8位
    //sps具体数据
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //封装pps,pps个数
    packet->m_body[i++] = 0x01;
    //pp长度，两个字节表示，需要将一个字节的int类型转换成两个字节
    packet->m_body[i++] = (pps_len >> 8);//高8位
    packet->m_body[i++] = pps_len & 0xFF;//低8位
    //PPS具体数据
    memcpy(&packet->m_body[i], pps, pps_len);

    //Packet类型为视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    //随意分配一个管道，尽量避开rtmp.c中使用的
    packet->m_nChannel = 10;

    //sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MINIMUM;
    if (videoCallback) {
        videoCallback(packet);
    }
}


void VideoChannel::sendFrame(int type, uint8_t *payload, int i_payload) {
    if (payload[2] == 0x00) {
        i_payload -= 4;
        payload += 4;
    } else {
        i_payload -= 3;
        payload += 3;
    }

    int bodySize = 9 + i_payload;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, bodySize);

    packet->m_body[0] = 0x27;//设置非关键帧类型
    if (type == NAL_SLICE_IDR) {//说明该NALU单元是关键帧的
        packet->m_body[0] = 0x17;
        LOGE("关键帧");
    }

    //设置类型
    packet->m_body[1] = 0x01;

    //设置时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    //四个字节的数据长度
    packet->m_body[5] = (i_payload >> 24) & 0xFF;
    packet->m_body[6] = (i_payload >> 16) & 0xFF;
    packet->m_body[7] = (i_payload >> 8) & 0xFF;
    packet->m_body[8] = (i_payload) & 0xFF;

    //图片数据
    memcpy(&packet->m_body[9], payload, i_payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    videoCallback(packet);

}