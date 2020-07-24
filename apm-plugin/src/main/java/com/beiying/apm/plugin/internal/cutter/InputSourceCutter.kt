package com.beiying.apm.plugin.internal.cutter

import com.android.build.api.transform.*
import com.beiying.apm.plugin.internal.ITask
import com.beiying.apm.plugin.internal.ThreadPool
import com.beiying.apm.plugin.internal.ajc.contentTypes
import com.beiying.apm.plugin.internal.ajc.scopes
import com.beiying.apm.plugin.internal.utils.*
import org.apache.commons.io.FileUtils

internal class InputSourceCutter(val transformInvocation: TransformInvocation, val fileFilter: FileFilter, val inputSourceFileStatus: InputSourceFileStatus) {
    val taskManager = ThreadPool()

    init {
        if (transformInvocation.isIncremental) {
            LogStatus.isIncremental("true")
            LogStatus.cutStart()

            transformInvocation.inputs.forEach { input ->
                //遍历whenDirInputsChanged的参数dirInput目录中发生变化的文件，
                // 然后调用fileFilter的whenAJClassChangedOfDir方法和whenClassChangedOfDir方法。
                // 如果属性inputSourceFileStatus中的isIncludeFileChanged变成true，即include files
                // 发生变化，则删除include输出jar；如果inputSourceFileStatus的isExcludeFileChanged为true，
                // 即exclude files发生变化，则重新生成exclude jar到输出目录。最后，调用mergeJar生成新的jar文件。
                input.directoryInputs.forEach { dirInput ->
                    whenDirInputsChanged(dirInput)
                }

                input.jarInputs.forEach { jarInput ->
                    whenJarInputsChanged(jarInput)
                }
            }

            LogStatus.cutEnd()
        } else {
            LogStatus.isIncremental("false")
            LogStatus.cutStart()

            transformInvocation.outputProvider.deleteAll()

            transformInvocation.inputs.forEach { input ->
                input.directoryInputs.forEach { dirInput ->
                    cutDirInputs(dirInput)
                }

                input.jarInputs.forEach { jarInput ->
                    cutJarInputs(jarInput)
                }
            }
            LogStatus.cutEnd()
        }
    }


    /**
     * 遍历参数dirInput中的每一个文件，调用fileFilter的filterAJClassFromDir和filterClassFromDir。
     * 如果exclude File Dir中的文件数大于0，就调用mergeJar，把这些exclude files放入jar文件。
     * */
    private fun cutDirInputs(dirInput: DirectoryInput) {
        taskManager.addTask(object : ITask {
            override fun call(): Any? {
                dirInput.file.eachFileRecurse { file ->
                    //过滤出AJ文件
                    fileFilter.filterAJClassFromDir(dirInput, file)
                    //过滤出class文件
                    fileFilter.filterClassFromDir(dirInput, file)
                }

                //put exclude files into jar
                if (countOfFiles(getExcludeFileDir()) > 0) {
                    val excludeJar = transformInvocation.outputProvider.getContentLocation("exclude", contentTypes as Set<QualifiedContent.ContentType>, scopes, Format.JAR)
                    mergeJar(getExcludeFileDir(), excludeJar)
                }
                return null
            }
        })
    }


    /**
     * 调用fileFilter的filterAJClassFromJar和filterClassFromJar方法，从jar文件中过滤掉AJ文件和class文件。
     * */
    private fun cutJarInputs(jarInput: JarInput) {
        taskManager.addTask(object : ITask {
            override fun call(): Any? {
                fileFilter.filterAJClassFromJar(jarInput)
                fileFilter.filterClassFromJar(transformInvocation, jarInput)
                return null
            }
        })
    }

    private fun whenDirInputsChanged(dirInput: DirectoryInput) {
        taskManager.addTask(object : ITask {
            override fun call(): Any? {
                dirInput.changedFiles.forEach { (file, status) ->
                    fileFilter.whenAJClassChangedOfDir(dirInput, file, status, inputSourceFileStatus)
                    fileFilter.whenClassChangedOfDir(dirInput, file, status, inputSourceFileStatus)
                }

                //如果include files 发生变化，则删除include输出jar
                if (inputSourceFileStatus.isIncludeFileChanged) {
                    logCore("whenDirInputsChanged include")
                    val includeOutputJar = transformInvocation.outputProvider.getContentLocation("include", contentTypes as Set<QualifiedContent.ContentType>, scopes, Format.JAR)
                    FileUtils.deleteQuietly(includeOutputJar)
                }

                //如果exclude files发生变化，则重新生成exclude jar到输出目录
                if (inputSourceFileStatus.isExcludeFileChanged) {
                    logCore("whenDirInputsChanged exclude")
                    val excludeOutputJar = transformInvocation.outputProvider.getContentLocation("exclude", contentTypes as Set<QualifiedContent.ContentType>?, scopes, Format.JAR)
                    FileUtils.deleteQuietly(excludeOutputJar)
                    mergeJar(getExcludeFileDir(), excludeOutputJar)
                }
                return null
            }
        })
    }

    /**
     * 如果参数jarInput的status不等于Status.NOTCHANGED，就是jarInput表示的jar文件发生了变化，
     * whenJarInputsChanged向taskManager中添加一个新的task。这个task依次调用fileFilterwhenAJClassChangedOfJar方法和whenClassChangedOfJar方法。
     * */
    private fun whenJarInputsChanged(jarInput: JarInput) {
        if (jarInput.status != Status.NOTCHANGED) {
            taskManager.addTask(object : ITask {
                override fun call(): Any? {
                    fileFilter.whenAJClassChangedOfJar(jarInput, inputSourceFileStatus)
                    fileFilter.whenClassChangedOfJar(transformInvocation, jarInput)
                    return null
                }
            })
        }
    }

    fun startCut() {
        taskManager.startWork()
    }
}