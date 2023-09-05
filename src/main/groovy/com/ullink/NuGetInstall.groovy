package com.ullink

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

abstract class NuGetInstall extends BaseNuGet {

    @Optional
    @Input
    abstract Property<String> getPackageId()

    @Optional
    @InputFile
    abstract RegularFileProperty getPackagesConfigFile()

    @Input
    abstract SetProperty<Object> getSources()

    @Optional
    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @Optional
    @Input
    abstract Property<String> getVersion()

    @Input
    abstract Property<Boolean> getIncludeVersionInPath()

    @Input
    abstract Property<Boolean> getPrerelease()

    @Input
    abstract Property<Boolean> getNoCache()

    @Input
    abstract Property<Boolean> getRequireConsent()

    @Optional
    @InputDirectory
    abstract DirectoryProperty getSolutionDirectory()

    @Optional
    @Input
    abstract Property<Object> getConflictAction()

    @Optional
    @InputFile
    abstract RegularFileProperty getConfigFile()

    NuGetInstall() {
        super('install')
        includeVersionInPath.convention(true)
        prerelease.convention(false)
        noCache.convention(false)
        requireConsent.convention(false)
    }

    void setPackagesConfigFile(String path) {
        packagesConfigFile.set(project.file(path))
    }

    void setOutputDirectory(String path) {
        outputDirectory.set(project.file(path))
    }

    void setSolutionDirectory(String path) {
        solutionDirectory.set(project.file(path))
    }

    void setConfigFile(String path) {
        configFile.set(project.file(path))
    }

    @Override
    void exec() {
        if (packageId.isPresent()) args packageId.get()
        if (packagesConfigFile.isPresent()) args packagesConfigFile.get()

        if (sources.isPresent()) args '-Source', sources.get().join(';')
        if (outputDirectory.isPresent()) args '-OutputDirectory', outputDirectory.get()
        if (version.isPresent()) args '-Version', version.get()
        if (!includeVersionInPath.isPresent()) args '-ExcludeVersion'
        if (prerelease.isPresent()) args '-Prerelease'
        if (noCache.isPresent()) args '-NoCache'
        if (requireConsent.isPresent()) args '-RequireConsent'
        if (solutionDirectory.isPresent()) args '-SolutionDirectory', solutionDirectory.get()
        if (conflictAction.isPresent()) args '-FileConflictAction', conflictAction.get()
        if (configFile.isPresent()) args '-ConfigFile', configFile.get()

        super.exec()
    }

}