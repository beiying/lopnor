package com.beiying.apm.plugin.internal

import com.beiying.apm.plugin.AppConstant
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies

val COMPILE_CONFIGUREATIONS = arrayOf("api", "compile")

fun Project.compatCompile(depLib: Any) {
    COMPILE_CONFIGUREATIONS.find { configurations.findByName(it) != null }?.let { dependencies.add(it, depLib) }
}

class ArgusDependencyResolutionListener(val project: Project) : DependencyResolutionListener {
    override fun beforeResolve(dependencies: ResolvableDependencies) {
        if (PluginConfig.argusApmConfig().dependencyEnabled) {
            if (PluginConfig.argusApmConfig().debugDependencies.isEmpty() && PluginConfig.argusApmConfig().moduleDependencies.isEmpty()) {
                project.compatCompile("com.qihoo360.argusapm:argus-apm-main:${AppConstant.VER}")
                project.compatCompile("com.qihoo360.argusapm:argus-apm-aop:${AppConstant.VER}")
                if (PluginConfig.argusApmConfig().okhttpEnabled) {
                    project.compatCompile("com.qihoo360.argusapm:argus-apm-okhttp:${AppConstant.VER}")
                }
            } else {
                //配置本地Module库， 方便断点调试
                if (PluginConfig.argusApmConfig().moduleDependencies.isNotEmpty()) {
                    PluginConfig.argusApmConfig().moduleDependencies.forEach{moduleLib: String ->
                        project.compatCompile(project.project(moduleLib))
                    }
                }

                //发布Release版本之前，可以使用debug库测试
                if(PluginConfig.argusApmConfig().debugDependencies.isNotEmpty()) {
                    project.repositories.mavenLocal()
                    //方便在测试的时候使用，不再需要单独的gradle发版本
                    PluginConfig.argusApmConfig().debugDependencies.forEach{ debugLib:String ->
                        project.compatCompile(debugLib)
                    }
                }
            }
        }
        project.gradle.removeListener(this)
    }

    override fun afterResolve(dependencies: ResolvableDependencies) {

    }
}