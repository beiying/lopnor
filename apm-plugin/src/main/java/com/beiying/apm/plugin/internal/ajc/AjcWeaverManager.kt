package com.beiying.apm.plugin.internal.ajc

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.TransformInvocation
import com.beiying.apm.plugin.internal.PluginConfig
import com.beiying.apm.plugin.internal.ThreadPool
import com.beiying.apm.plugin.internal.cutter.InputSourceFileStatus
import com.beiying.apm.plugin.internal.cutter.getAspectDir
import com.beiying.apm.plugin.internal.cutter.getExcludeFileDir
import com.beiying.apm.plugin.internal.cutter.getIncludeFileDir
import com.beiying.apm.plugin.internal.utils.filterJar
import com.beiying.apm.plugin.internal.utils.log
import com.beiying.apm.plugin.internal.utils.logCore
import org.apache.commons.io.FileUtils
import java.io.File

val contentTypes = mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
val scopes = mutableSetOf(QualifiedContent.Scope.EXTERNAL_LIBRARIES)

class AjcWeaverManager(private val transformInvocation: TransformInvocation, private val inputSourceFileStatus: InputSourceFileStatus) {
    private val threadPool = ThreadPool()
    private val aspectPath = arrayListOf<File>()
    private val classPath = arrayListOf<File>()

    fun weaver() {
        //设置系统属性aspectj.multithreaded为true，允许多线程运行aspectj
        System.setProperty("aspectj.multithreaded", "true")

        if (transformInvocation.isIncremental) {//支持增量编译，那么调用方法createIncrementalTask，创建增量编译任务
            createIncrementalTask()
        } else {//创建普通（全量）编译任务。
            createTask()
        }

        log("AjcWeaverList.size is ${threadPool.taskList.size}")

        aspectPath.add(getAspectDir())
        classPath.add(getIncludeFileDir())
        classPath.add(getExcludeFileDir())

        //设置每一个条目AjcWeaver的encoding，aspectPath，classPath，targetCompatibility,sourceCompatibility,bootClassPath(启动类)，ajcArgs(aspectj的参数)
        threadPool.taskList.forEach { ajcWeaver ->
            ajcWeaver as AjcWeaver
            ajcWeaver.encoding = PluginConfig.encoding
            ajcWeaver.aspectPath = aspectPath
            ajcWeaver.classPath = classPath
            ajcWeaver.targetCompatibility = PluginConfig.targetCompatibility
            ajcWeaver.sourceCompatibility = PluginConfig.targetCompatibility
            ajcWeaver.bootClassPath = PluginConfig.bootClassPath
            ajcWeaver.ajcArgs = PluginConfig.argusApmConfig().ajcArgs
        }
        threadPool.startWork()
    }

    private fun createTask() {

        val ajcWeaver = AjcWeaver()
        val includeJar = transformInvocation.outputProvider.getContentLocation("include", contentTypes as Set<QualifiedContent.ContentType>, scopes, Format.JAR)
        //获取include jar文件includeJar，如果includeJar的父目录不存在，就创建它的父目录。。h，
        //。调用addAjcWeaver(ajcWeaver)，实际上。。，
        if (!includeJar.parentFile.exists()) {
            FileUtils.forceMkdir(includeJar.parentFile)
        }
        FileUtils.deleteQuietly(includeJar)//删除includeJar
        ajcWeaver.outputJar = includeJar.absolutePath//ajcWeaver的outputJar设置成includeJar的absolutePath
        ajcWeaver.inPath.add(getIncludeFileDir())//// 将Include file目录添加到ajcWeaver的inPath列表
        addAjcWeaver(ajcWeaver)//把ajcWeaver添加到threadPool的task列表

        //遍历参数transformInvocation的inputs列表，对每一个item，遍历它的jarInputs列表，把每一个jar文件添加到classPath列表
        //然后调用filterJar方法，判断该jar文件是否要参与AJC织入。如果要参与，首先生成一个AjcWeaver的对象tempAjcWeaver，获取output jar文件，
        //如果outputJar文件的父目录不存在，首先创建该目录。把tempAjcWeaver的outputJar设成outputJar的绝对路径,
        //然后调用addAjcWeaver把tempAjcWeaver添加到threadPool的taskList。
        transformInvocation.inputs.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                classPath.add(jarInput.file)
                //如果该Jar参与AJC织入的话，则进行下面操作
                if (filterJar(jarInput, PluginConfig.argusApmConfig().includes, PluginConfig.argusApmConfig().excludes, PluginConfig.argusApmConfig().excludeJars)) {
                    val tempAjcWeaver = AjcWeaver()
                    tempAjcWeaver.inPath.add(jarInput.file)

                    val outputJar = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                    if (!outputJar.parentFile?.exists()!!) {
                        outputJar.parentFile?.mkdirs()
                    }

                    tempAjcWeaver.outputJar = outputJar.absolutePath
                    addAjcWeaver(tempAjcWeaver)
                }

            }
        }

    }

    private fun createIncrementalTask() {
        //如果AJ或者Include文件有一个变化的话,则重新织入
        //首先判断aspect文件或者include文件有没有变化，只要有一个发生变化，就需要重新进行“织入”。
        // 生成一个新的AjcWeaver对象ajcWeaver，从output provider中获取输出jar文件，然后删除这个jar，
        // 把ajcWeaver的outputJar设置成这个jar文件的绝对路径，把include目录的路径，加入到ajcWeaver的inPath列表中。
        // 调用addAjcWeaver(ajcWeaver)把ajcWeaver加入到threadPool的taskList中。
        if (inputSourceFileStatus.isAspectChanged || inputSourceFileStatus.isIncludeFileChanged) {
            val ajcWeaver = AjcWeaver()
            val outputJar = transformInvocation.outputProvider?.getContentLocation("include", contentTypes as Set<QualifiedContent.ContentType>, scopes, Format.JAR)
            FileUtils.deleteQuietly(outputJar)

            ajcWeaver.outputJar = outputJar?.absolutePath
            ajcWeaver.inPath.add(getIncludeFileDir())
            addAjcWeaver(ajcWeaver)

            logCore("createIncrementalTask isAspectChanged: [ ${inputSourceFileStatus.isAspectChanged} ]    isIncludeFileChanged:  [ ${inputSourceFileStatus.isIncludeFileChanged} ]")
        }


        // 遍历transformInvocation重的inputs列表，对inputs中的每一个item，遍历它的jarInputs列表，把每一个jarInput的file属性（File类型）添加到classpath中。
        // 从transformInvocation的output provider中获取outputJar文件，如果outputJar的父路径不存在，首先创建它的父目录。调用filterJar判断是否有匹配的includes，
        // excludes path或者exclude jar包。如果存在，并且有aspectj文件发生改变，首先删除前边获取的outputJar文件， 生成一个AjcWeaver的对象tempAjcWeaver，
        // 把jarInputs中当前的条目的file（File类型）属性，添加到tempAjcWeaver的inPath列表中，outputJar的绝对路径absolutePath，然后调用addAjcWeaver(tempAjcWeaver)
        // 把tempAjcWeaver添加到threadPool的taskList列表中。如果没有aspectj文件发生改变，并且outputJar不存在，同样先生成一个AjcWeaver的对象tempAjcWeaver，
        // 然后把jarInput的file属性，添加到tempAjcWeaver的inPath列表。tempAjcWeaver的outputJar设置成outputJar的绝对路径absolutePath，
        // 然后调用addAjcWeaver(tempAjcWeaver)把tempAjcWeaver添加到threadPool的taskList列表中
        transformInvocation.inputs?.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                classPath.add(jarInput.file)
                val outputJar = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                if (!outputJar.parentFile?.exists()!!) {
                    outputJar.parentFile?.mkdirs()
                }

                if (filterJar(jarInput, PluginConfig.argusApmConfig().includes, PluginConfig.argusApmConfig().excludes, PluginConfig.argusApmConfig().excludeJars)) {
                    if (inputSourceFileStatus.isAspectChanged) {
                        FileUtils.deleteQuietly(outputJar)

                        val tempAjcWeaver = AjcWeaver()
                        tempAjcWeaver.inPath.add(jarInput.file)
                        tempAjcWeaver.outputJar = outputJar.absolutePath
                        addAjcWeaver(tempAjcWeaver)

                        logCore("jar inputSourceFileStatus.isAspectChanged true")
                    } else {
                        if (!outputJar.exists()) {
                            val tempAjcWeaver = AjcWeaver()
                            tempAjcWeaver.inPath.add(jarInput.file)
                            tempAjcWeaver.outputJar = outputJar.absolutePath
                            addAjcWeaver(tempAjcWeaver)
                            logCore("jar inputSourceFileStatus.isAspectChanged false && outputJar.exists() is false")
                        }
                    }
                }
            }
        }
    }

    private fun addAjcWeaver(ajcWeaver: AjcWeaver) {
        threadPool.addTask(ajcWeaver)
    }
}