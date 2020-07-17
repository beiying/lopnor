package com.beiying.apm.plugin.internal

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project

class PluginConfig {
    companion object {
        lateinit var project: Project
        lateinit var encoding: String
        lateinit var bootClassPath: String
        lateinit var sourceCompatibility: String
        lateinit var targetCompatibility: String

        fun init(project: Project) {
            val hasAppPlugin = project.plugins.hasPlugin(AppPlugin::class.java)
            val hasLibPlugin = project.plugins.hasPlugin(LibraryPlugin::class.java)

            if (!hasAppPlugin && !hasAppPlugin) {
                throw  GradleException("argusapm: The 'com.android.application' or 'com.android.library' plugin is required.")
            }
        }
    }
}