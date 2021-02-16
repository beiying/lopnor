package com.beiying.lopnor.demo.navigation

data class BottomBar(var selectTab: Int, val tabs: List<Tab>) {
    data class Tab(
        val size: Int,
        var enable: Boolean,
        val index: Int,
        val pageUrl: String,
        var title: String
    ) {

    }
}