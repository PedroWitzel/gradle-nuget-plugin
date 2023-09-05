package com.ullink

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional

abstract class NuGetSources extends BaseNuGet {

    enum Operation {
        add, remove, enable, disable, list, update,
    }

    @Input
    abstract Property<Operation> getOperation()

    @Optional
    @Input
    abstract Property<String> getSourceName()

    @Optional
    @Input
    abstract Property<String> getSourceUrl()

    @Optional
    @Input
    abstract Property<String> getUsername()

    @Optional
    @Input
    abstract Property<String> getPassword()

    @Optional
    @InputFile
    abstract RegularFileProperty getConfigFile()

    @Input
    abstract Property<Boolean> getStorePasswordInClearText()

    NuGetSources() {
        super('sources')
        storePasswordInClearText.convention(false)
    }

    @Override
    void exec() {
        if (!operation.isPresent()) {
            throw new GradleException('Operation not specified for NuGetSources task.')
        }
        if (operation.get() != Operation.list && !sourceName.isPresent()) {
            throw new GradleException('SourceName not specified for NuGetSources task.')
        }

        args operation.get()
        if (sourceName.isPresent()) args '-Name', sourceName.get()
        if (sourceUrl.isPresent()) args '-Source', sourceUrl.get()
        if (username.isPresent()) args '-UserName', username.get()
        if (password.isPresent()) args '-Password', password.get()
        if (configFile.isPresent()) args '-ConfigFile', configFile.get()
        if (storePasswordInClearText.isPresent() && storePasswordInClearText.get()) args '-StorePasswordInClearText'
        super.exec()
    }

}