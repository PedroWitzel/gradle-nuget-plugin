package com.ullink

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin

class NuGetPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.apply plugin: 'nuget-base'

        project.tasks.register('nugetRestore', NuGetRestore) {
            group = BasePlugin.BUILD_GROUP
            description = 'Restores the configured config file or solution directory.'
        }

        def nugetSpec = project.tasks.register('nugetSpec', NuGetSpec) {
            group = BasePlugin.BUILD_GROUP
            description = 'Generates the NuGet spec file.'
        }

        def nugetPack = project.tasks.register('nugetPack', NuGetPack) {
            group = BasePlugin.BUILD_GROUP
            description = 'Creates the NuGet package with the configured spec file.'
            dependsOn nugetSpec
        }

        project.tasks.register('nugetPush', NuGetPush) {
            group = 'publishing'
            dependsOn nugetPack
            description = 'Pushes the NuGet package to the configured server url.'
        }

        project.tasks.register('nugetSources', NuGetSources) {
            group = BasePlugin.BUILD_GROUP
            description = 'Adds, removes, enables, disables and lists nuget sources (feeds).'
        }
    }
}

