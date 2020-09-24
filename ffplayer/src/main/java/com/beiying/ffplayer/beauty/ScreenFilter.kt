package com.beiying.ffplayer.beauty

import android.content.Context
import com.beiying.ffplayer.R

//作为显示滤镜, 接收CameraFilter已经渲染好的特效
class ScreenFilter(context: Context)
    : AbstractFilter(context, R.raw.base_vertex, R.raw.base_frag) {
    override fun initCoordinate() {
    }

}