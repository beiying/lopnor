package com.beiying.lopnor.base.restful.annotation

/**
 * @GET("/cities/all")
 *fun test(@Filed("province") int provinceId)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GET(val value: String)