package com.beiying.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DemoPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        TODO("Not yet implemented")
        (target.extensions.getByName("android") as AppExtension).registerTransform(DemoTransform())
    }
}