package com.ullink

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

abstract class NuGetPush extends BaseNuGet {

    @Optional
    @InputFile
    abstract RegularFileProperty getNupkgFile()

    @Optional
    @Input
    abstract Property<String> getServerUrl()

    @Optional
    @Input
    abstract Property<String> getApiKey()

    @Optional
    @InputFile
    abstract RegularFileProperty getConfigFile()

    NuGetPush() {
        super('push')

        // Force always execute
        outputs.upToDateWhen { false }
    }

    @Optional
    @InputFile
    File getNugetPackOutputFile() {
        if (dependentNuGetPack) dependentNuGetPack.packageFile
    }

    @Internal
    NuGetPack getDependentNuGetPack() {
        dependsOn.find { it instanceof NuGetPack && it.enabled } as NuGetPack
    }

    @Override
    void exec() {
        args nupkgFile.isPresent() ? nupkgFile.get().asFile.path : nugetPackOutputFile

        if (serverUrl.isPresent()) args '-Source', serverUrl.get()
        if (apiKey.isPresent()) args '-ApiKey', apiKey.get()
        if (configFile.isPresent()) args '-ConfigFile', configFile.get().asFile.path

        super.exec()
    }
}
