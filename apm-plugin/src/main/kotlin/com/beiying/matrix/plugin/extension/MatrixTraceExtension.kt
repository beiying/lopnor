package com.beiying.matrix.plugin.extension

open class MatrixTraceExtension {
    var isTransformInjectionForced = false

    var baseMethodMapFile: String = ""
    var blackListFile: String = ""
    var customDexTransformName: String = ""
    var isEnable = false

    override fun toString(): String {
        return """|
            | baseMethodMapFile = ${baseMethodMapFile}
            | blackListFile = ${blackListFile}
            | customDexTransformName = ${customDexTransformName}
        """.trimIndent()
    }
}