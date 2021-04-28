package com.beiying.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.beiying.apm.plugin.internal.utils.eachFileRecurse
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream

class DemoTransform: Transform() {
    override fun getName(): String {
        return ""
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(transformInvocation: TransformInvocation) {
        //当前编译对于Transform是否增量编译受两方面的影响
        //（1）isIncremental方法的返回值；
        // (2) 当前编译是否有增量编译基础（clean之后的第一次编译没有增量基础，之后的编译有增量基础）
        var mIsIncremental = isIncremental && transformInvocation.isIncremental
        var outputProvider = transformInvocation.outputProvider
        if (!mIsIncremental) {
            outputProvider.deleteAll()
        }
        transformInvocation.inputs.forEach { input ->
            input.directoryInputs.forEach { dirInput ->
                val dest = outputProvider.getContentLocation(dirInput.name,
                    dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                dirInput.file.eachFileRecurse { file ->
                }
                FileUtils.copyDirectory(dirInput.file, dest)
            }

            input.jarInputs.forEach { jarInput ->
                val dest = outputProvider.getContentLocation(jarInput.name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyDirectory(jarInput.file, dest)
            }
        }
    }

    fun modifyClass(inputStream: InputStream) {
//        val classFile: ClassFile = ClassFile(DataInputStream(BufferedInputStream(inputStream)))

    }
}