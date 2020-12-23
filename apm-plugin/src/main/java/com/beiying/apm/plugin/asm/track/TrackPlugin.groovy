package com.beiying.apm.plugin.asm.track

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TrackPlugin implements Plugin<Project> {
    void apply(Project project) {

        TrackExtension extension = project.extensions.create("sensorsAnalytics", TrackExtension)

        boolean disableSensorsAnalyticsPlugin = false
        Properties properties = new Properties()
        if (project.rootProject.file('gradle.properties').exists()) {
            properties.load(project.rootProject.file('gradle.properties').newDataInputStream())
            disableSensorsAnalyticsPlugin = Boolean.parseBoolean(properties.getProperty("sensorsAnalytics.disablePlugin", "false"))
        }

        if (!disableSensorsAnalyticsPlugin) {
            AppExtension appExtension = project.extensions.findByType(AppExtension.class)
            appExtension.registerTransform(new TrackTransform(project, extension))
        } else {
            println("------------您已关闭了神策插件--------------")
        }
    }
}