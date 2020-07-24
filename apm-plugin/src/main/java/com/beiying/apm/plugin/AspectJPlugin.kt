package com.beiying.apm.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.beiying.apm.plugin.internal.ArgusDependencyResolutionListener
import com.beiying.apm.plugin.internal.BuildTimeListener
import com.beiying.apm.plugin.internal.PluginConfig
import com.beiying.apm.plugin.internal.compatCompile
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class AspectJPlugin : Plugin<Project> {
    private lateinit var mProject: Project
    override fun apply(project: Project) {
        mProject = project
        project.extensions.create(AppConstant.USER_CONFIG, ArgusApmConfig::class.java)

        //公共配置初始化，方便获取公共信息
        PluginConfig.init(project)

        //自定义依赖库管理
        project.gradle.addListener(ArgusDependencyResolutionListener(project))

        project.repositories.mavenCentral()
        project.compatCompile("org.aspectj:aspectjrt:1.8.9")

        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            project.gradle.addListener(BuildTimeListener())

            //在编译流程中会通过TaskManager#createPostCompilationTasks为这个自定义的Transform生成一个对应的Task，
            // （transformClassesWithAspectJTransformForDebug），在.class文件转换成.dex文件的流程中会执行这个Task，
            // 对所有的.class文件（可包括第三方库的.class）进行转换，转换的逻辑定义在Transform的transform方法中。
            //
            val android = project.extensions.getByType(AppExtension::class.java)
            android.registerTransform(AspectJTransform(project))
        }

    }

}