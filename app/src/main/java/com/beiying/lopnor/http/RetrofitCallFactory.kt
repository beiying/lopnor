package com.beiying.lopnor.http

import com.beiying.lopnor.base.restful.ByCall
import com.beiying.lopnor.base.restful.ByHttpCallback
import com.beiying.lopnor.base.restful.ByRequest
import com.beiying.lopnor.base.restful.ByResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap
import retrofit2.http.Url

class RetrofitCallFactory: ByCall.CallFactory {
    private val retrofitApiService: RetrofitApiService
    init {
        val retrofit = Retrofit.Builder().baseUrl("").build()
        retrofitApiService = retrofit.create(RetrofitApiService::class.java)

    }
    override fun newCall(request: ByRequest): ByCall<Any> {
        return RetrofitCall(request)
    }

    inner class RetrofitCall(val request: ByRequest): ByCall<Any> {
        override fun execute(): ByResponse<Any> {
            var call: Call<ResponseBody>? = null
            if (request.httpMethod == ByRequest.METHOD.GET) {
                call = retrofitApiService.get(request.headers, request.endPointUrl(), request.parameters)
            }
            return ByResponse()
        }

        override fun equeue(callback: ByHttpCallback<Any>) {
            TODO("Not yet implemented")
        }

    }

    interface RetrofitApiService {
        @GET
        fun get(@HeaderMap headers: MutableMap<String, String>?, @Url url: String, @QueryMap(encoded = true) params: MutableMap<String, String>?): Call<ResponseBody>
    }
}