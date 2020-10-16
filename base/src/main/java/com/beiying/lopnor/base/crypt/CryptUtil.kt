package com.beiying.lopnor.base.crypt

import android.content.Context

class CryptUtil {
    companion object {
        init {
            System.loadLibrary("crypt.so")
        }
    }

    /**
     * 通过native方法签名http请求参数
     * */
    external fun signatureParams(params: String): String

    /**
     * 校验签名，只允许自己的app可以使用crypt.so
     **/
    external fun signatureVerify(context: Context)
}