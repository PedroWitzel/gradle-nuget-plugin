package com.ullink

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input


abstract class NugetLocalsClear extends BaseNuGet {

    @Input
    abstract Property<Boolean> getAll()

    NugetLocalsClear() {
        super('locals')
        all.convention(false)
    }

    @Override
    void exec() {
        args '-clear'
        if (all.getOrNull()) args 'all'
        super.exec()
    }

}
