package com.beiying.apm.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.beiying.apm.plugin.internal.PluginConfig
import com.beiying.apm.plugin.internal.ajc.AjcWeaverManager
import com.beiying.apm.plugin.internal.cutter.FileFilter
import com.beiying.apm.plugin.internal.cutter.InputSourceCutter
import com.beiying.apm.plugin.internal.cutter.InputSourceFileStatus
import com.beiying.apm.plugin.internal.utils.LogStatus
import com.beiying.apm.plugin.internal.utils.outputFiles
import com.google.common.collect.Sets
import org.gradle.api.Project

internal class AspectJTransform(private val project: Project) : Transform() {
    override fun getName(): String {
        return AppConstant.TRANSFORM_NAME
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return Sets.immutableEnumSet(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun transform(transformInvocation: TransformInvocation) {
        val transformTask = transformInvocation.context as TransformTask
        LogStatus.logStart(transformTask.variantName)

        //第一步：对输入源Class文件进行切割分组
        val fileFilter = FileFilter(project, transformTask.variantName)
        val inputSourceFileStatus = InputSourceFileStatus()
        InputSourceCutter(transformInvocation, fileFilter, inputSourceFileStatus).startCut()

        //第二步：如果含有AspectJ文件,则开启植入;否则,将输入源输出到目标目录下
        //首先判断PluginConfig.argusApmConfig().enabled是否为true，即argus apm有没有使能，
        //同时判断第一步生成的fileFilter是否包含aspect 文件。如果两个条件都满足，就用transformInvocation和inputSourceFileStatus
        //生成一个AjcWeaverManager的对象，并且调用该对象的weaver方法。如果前边的两个条件有一个不满足，调用outputFiles(transformInvocation)输出文件的内容。
        if (PluginConfig.argusApmConfig().enabled && fileFilter.hasAspectJFile()) {
            AjcWeaverManager(transformInvocation, inputSourceFileStatus).weaver()
        } else {
            outputFiles(transformInvocation)
        }

        LogStatus.logEnd(transformTask.variantName)
    }
}