package com.beiying.lopnor.base.restful

import java.lang.reflect.Proxy

class ByRestful(val baseUrl: String, callFactory: ByCall.CallFactory) {
    private var interceptors: MutableList<ByInterceptor> = mutableListOf()

    fun addInterceptor(interceptor: ByInterceptor) {
        interceptors.add(interceptor)
    }

    fun <T> create(service: Class<T>): T {
        return Proxy.newProxyInstance(service.classLoader, arrayOf<Class<*>>(service)) {
            proxy, method, args ->
            MethodParser.parse(baseUrl, method)
        } as T
    }
}