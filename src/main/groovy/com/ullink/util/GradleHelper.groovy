package com.ullink.util

import org.gradle.api.Project

class GradleHelper {
    static String getPropertyFromTask(Project project, String property, String task) {
        try {
            return project.tasks.named(task).get().property(property)
        } catch (ignored) {
            return null
        }
    }
}
