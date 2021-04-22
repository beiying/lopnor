package com.beiying.matrix.plugin.trace

import com.beiying.javalib.common.utils.FileUtil
import com.beiying.javalib.common.utils.Util
import com.beiying.matrix.plugin.TraceBuildConstants
import com.beiying.matrix.plugin.retrace.MappingCollector
import java.util.*

class Configuration {
    var packageName: String? = null
    var mappingDir: String = ""
    var baseMethodMapPath: String? = null
    var methodMapFilePath: String? = null
    var ignoreMethodMapFilePath: String? = null
    var blockListFilePath: String? = null
    var traceClassOut: String? = null
    var blockSet = HashSet<String>()

    constructor() {}

    constructor(
        packageName: String?,
        mappingDir: String,
        baseMethodMapPath: String?,
        methodMapFilePath: String?,
        ignoreMethodMapFilePath: String?,
        blockListFilePath: String?,
        traceClassOut: String?
    ) {
        this.packageName = packageName
        this.mappingDir = mappingDir
        this.baseMethodMapPath = Util.nullAsNil(baseMethodMapPath)
        this.methodMapFilePath = Util.nullAsNil(methodMapFilePath)
        this.ignoreMethodMapFilePath = Util.nullAsNil(ignoreMethodMapFilePath)
        this.blockListFilePath = Util.nullAsNil(blockListFilePath)
        this.traceClassOut = Util.nullAsNil(traceClassOut)
    }

    fun parseBlockFile(processor: MappingCollector): Int {
        val blockStr: String =
            TraceBuildConstants.DEFAULT_BLOCK_TRACE + FileUtil.readFileAsString(blockListFilePath)
        val blockArray =
            blockStr.trim { it <= ' ' }.replace("/", ".").split("\n").toTypedArray()
        if (blockArray != null) {
            for (block in blockArray) {
                if (block.isEmpty()) {
                    continue
                }
                if (block.startsWith("#")) {
                    continue
                }
                if (block.startsWith("[")) {
                    continue
                }
                if (block.startsWith("-keepclass ")) {
                    var newBlock = block.replace("-keepclass ", "")
                    blockSet.add(processor.proguardClassName(newBlock, newBlock))
                } else if (block.startsWith("-keeppackage ")) {
                    var newBlock = block.replace("-keeppackage ", "")
                    blockSet.add(processor.proguardPackageName(newBlock, newBlock))
                }
            }
        }
        return blockSet.size
    }

    override fun toString(): String {
        return """
            
            # Configuration
            |* packageName:	$packageName
            |* mappingDir:	$mappingDir
            |* baseMethodMapPath:	$baseMethodMapPath
            |* methodMapFilePath:	$methodMapFilePath
            |* ignoreMethodMapFilePath:	$ignoreMethodMapFilePath
            |* blockListFilePath:	$blockListFilePath
            |* traceClassOut:	$traceClassOut
            
            """.trimIndent()
    }

    class Builder {
        var packageName: String? = null
        var mappingPath: String = ""
        var baseMethodMap: String? = null
        var methodMapFile: String? = null
        var ignoreMethodMapFile: String? = null
        var blockListFile: String? = null
        var traceClassOut: String? = null
        fun setPackageName(packageName: String?): Builder {
            this.packageName = packageName
            return this
        }

        fun setMappingPath(mappingPath: String): Builder {
            this.mappingPath = mappingPath
            return this
        }

        fun setBaseMethodMap(baseMethodMap: String?): Builder {
            this.baseMethodMap = baseMethodMap
            return this
        }

        fun setTraceClassOut(traceClassOut: String?): Builder {
            this.traceClassOut = traceClassOut
            return this
        }

        fun setMethodMapFilePath(methodMapDir: String?): Builder {
            methodMapFile = methodMapDir
            return this
        }

        fun setIgnoreMethodMapFilePath(methodMapDir: String?): Builder {
            ignoreMethodMapFile = methodMapDir
            return this
        }

        fun setBlockListFile(blockListFile: String?): Builder {
            this.blockListFile = blockListFile
            return this
        }

        fun build(): Configuration {
            return Configuration(
                packageName,
                mappingPath,
                baseMethodMap,
                methodMapFile,
                ignoreMethodMapFile,
                blockListFile,
                traceClassOut
            )
        }
    }
}