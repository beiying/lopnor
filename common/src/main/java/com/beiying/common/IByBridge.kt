package com.beiying.common

interface IByBridge<P, Callback> {
    fun onBack(p: P?)

    fun gotoNative(p: P)

    fun getHeaderParams(callback: Callback)
}