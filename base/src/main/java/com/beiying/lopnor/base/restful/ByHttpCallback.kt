package com.beiying.lopnor.base.restful

interface ByHttpCallback<T> {
    fun onSuccess(response: ByResponse<T>)

    fun onFailure(throwable: Throwable)
}