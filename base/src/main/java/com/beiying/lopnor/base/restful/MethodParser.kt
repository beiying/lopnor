package com.beiying.lopnor.base.restful

import com.beiying.lopnor.base.restful.annotation.*
import java.lang.IllegalStateException
import java.lang.reflect.*

class MethodParser(
    val baseUrl: String,
    method: Method
) {

    private var replaceRelativeUrl: String? = null
    private var cacheStrategy: Int = CacheStrategy.NET_ONLY
    private var domainUrl: String? = null
    private var formPost: Boolean = true
    private var httpMethod: Int = -1
    private lateinit var relativeUrl: String
    private lateinit var returnType: Type
    private var headers: MutableMap<String, String> = mutableMapOf()
    private var parameters: MutableMap<String, String> = mutableMapOf()

    init {
        parseMethodReturnType(method)

        parseMethodParameters(method)

        parseMethodAnntations(method)
    }


    /**
     * interface ApiService {
     *  @Headers("auth-token:token", "accountId:123456")
     *  @BaseUrl("https://api.devio.org/as/")
     *  @POST("/cities/{province}")
     *  @GET("/cities")
     * fun listCities(@Path("province") province: Int,@Filed("page") page: Int): HiCall<JsonObject>
     * }
     */
    private fun parseMethodAnntations(method: Method) {
        val annotations = method.annotations;
        for (annotation in annotations) {
            if (annotation is GET) {
                relativeUrl = annotation.value
                httpMethod = ByRequest.METHOD.GET
            } else if (annotation is POST) {
                relativeUrl = annotation.value
                httpMethod = ByRequest.METHOD.POST
                formPost = annotation.formPost
            } else if (annotation is PUT) {
                formPost = annotation.formPost
                httpMethod = ByRequest.METHOD.PUT
                relativeUrl = annotation.value
            } else if (annotation is DELETE) {
                httpMethod = ByRequest.METHOD.DELETE
                relativeUrl = annotation.value
            } else if (annotation is Headers) {
                val headersArray = annotation.value
                //@Headers("auth-token:token", "accountId:123456")
                for (header in headersArray) {
                    val colon = header.indexOf(":")
                    check(!(colon == 0 || colon == -1)) {
                        String.format(
                            "@headers value must be in the form [name:value] ,but found [%s]",
                            header
                        )
                    }
                    val name = header.substring(0, colon)
                    val value = header.substring(colon + 1).trim()
                    headers[name] = value
                }
            } else if (annotation is BaseUrl) {
                domainUrl = annotation.value
            } else if (annotation is CacheStrategy) {
                cacheStrategy = annotation.value
            } else {
                throw IllegalStateException("cannot handle method annotation:" + annotation.javaClass.toString())
            }
        }

        require((httpMethod == ByRequest.METHOD.GET)
                || (httpMethod == ByRequest.METHOD.POST
                || (httpMethod == ByRequest.METHOD.PUT)
                || (httpMethod == ByRequest.METHOD.DELETE))) {
            String.format("method %s must has one of GET,POST,PUT,DELETE ", method.name)
        }

        if (domainUrl == null) {
            domainUrl = baseUrl
        }
    }

    private fun parseMethodParameters(method: Method) {
        TODO("Not yet implemented")
    }

    private fun parseMethodReturnType(method: Method) {
        if (method.returnType != ByCall::class.java) {
            throw IllegalStateException(
                String.format(
                    "method %s must be type of HiCall.class",
                    method.name
                )
            )
        }
        val genericReturnType = method.genericReturnType
        if (genericReturnType is ParameterizedType) {
            val actualTypeArguments = genericReturnType.actualTypeArguments
            require(actualTypeArguments.size == 1) { "method %s can only has one generic return type" }
            val argument = actualTypeArguments[0]
            require(validateGenericType(argument)) {
                String.format("method %s generic return type must not be an unknown type. " + method.name)
            }
            returnType = argument
        } else {
            throw  IllegalStateException(
                String.format(
                    "method %s must has one gerneric return type",
                    method.name
                )
            )
        }
    }

    private fun validateGenericType(type: Type): Boolean {
        /**
         *wrong
         *  fun test():HiCall<Any>
         *  fun test():HiCall<List<*>>
         *  fun test():HiCall<ApiInterface>
         *expect
         *  fun test():HiCall<User>
         */
        //如果指定的泛型是集合类型的，那还检查集合的泛型参数
        if (type is GenericArrayType) {
            return validateGenericType(type.genericComponentType)
        }
        //如果指定的泛型是一个接口 也不行
        if (type is TypeVariable<*>) {
            return false
        }
        //如果指定的泛型是一个通配符 ？extends Number 也不行
        if (type is WildcardType) {
            return false
        }

        return true
    }

    companion object {
        fun parse(baseUrl: String, method: Method): MethodParser {
            return MethodParser(baseUrl, method)
        }
    }
}