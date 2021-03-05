package com.beiying.lopnor.base.restful

import java.io.IOException


interface ByCall<T> {
    @Throws(IOException::class)
    fun execute(): ByResponse<T>

    fun equeue(callback: ByHttpCallback<T>)

    interface CallFactory {
        fun newCall(request: ByRequest): ByCall<*>
    }
}