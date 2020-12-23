package com.beiying.apm.plugin.asm

import com.android.build.api.transform.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import java.io.File

class AsmTransform : Transform() {
    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        TODO("Not yet implemented")
    }

    override fun isIncremental(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        TODO("Not yet implemented")
    }

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
    }

    override fun transform(
        context: Context,
        inputs: MutableCollection<TransformInput>,
        referencedInputs: MutableCollection<TransformInput>,
        outputProvider: TransformOutputProvider,
        isIncremental: Boolean
    ) {
        // Transform 的 inputs 有两种类型，一种是目录，一种是 jar 包，要分开遍历
        inputs.forEach { input ->
            //遍历目录
            input.directoryInputs.forEach { directoryInput ->
                //获取 output 目录
                val dest = outputProvider.getContentLocation(directoryInput.name,
                directoryInput.contentTypes, directoryInput.scopes,
                Format.DIRECTORY)
                // 将 input 的目录复制到 output 指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            //遍历 jar
            input.jarInputs.forEach { jarInput ->
                // 重命名输出文件（同目录copyFile会冲突）
                var jarName = jarInput.name
                val md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length - 4)
                }

                val copyJarFile: File = jarInput.file

                //生成输出路径
                val dest = outputProvider.getContentLocation(jarName + md5Name,
                jarInput.contentTypes, jarInput.scopes, Format.JAR)
                // 将 input 的目录复制到 output 指定目录
                FileUtils.copyFile(copyJarFile, dest)
            }
        }

    }
}