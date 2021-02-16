package com.beiying.lopnor.demo.navigation

data class ByNavDestination(
    val pageUrl: String,
    val asStarter: Boolean,
    val id: Int,
    val destType: String,
    val clazzName: String
) {
}