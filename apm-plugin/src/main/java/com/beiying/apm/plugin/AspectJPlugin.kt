package com.beiying.apm.plugin

import com.beiying.apm.plugin.internal.PluginConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class AspectJPlugin : Plugin<Project> {
    private lateinit var mProject: Project
    override fun apply(project: Project) {
        mProject = project
        project.extensions.create(AppConstant.USER_CONFIG, ArgusApmConfig::class.java)

        PluginConfig.init(project)
    }

}