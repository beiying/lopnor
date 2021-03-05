package com.beiying.lopnor.base.restful.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class POST(val value: String, val formPost: Boolean = true) {

}