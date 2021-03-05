package com.beiying.lopnor.base.restful

interface ByInterceptor {
    fun intercept(chain: Chain)

    interface Chain {
        fun request(): ByRequest

        fun response(): ByResponse<*>?
    }
}