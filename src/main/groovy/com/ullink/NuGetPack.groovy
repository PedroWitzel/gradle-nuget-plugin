package com.ullink

import com.ullink.util.GradleHelper
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.apache.commons.io.FilenameUtils
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class NuGetPack extends BaseNuGet {

    @Internal
    File nuspecFile

    @Optional
    @InputFile
    abstract RegularFileProperty getCsprojPath()

    @OutputDirectory
    abstract DirectoryProperty getDestinationDir()

    @Optional
    @InputFile
    abstract RegularFileProperty getBasePath()

    @Optional
    @Input
    abstract Property<Object> getPackageVersion()

    @Optional
    @Input
    abstract Property<Object> getExclude()

    @Input
    abstract Property<Object> getGenerateSymbols()

    @Input
    abstract Property<Object> getTool()

    @Input
    abstract Property<Object> getBuild()

    @Input
    abstract Property<Object> getDefaultExcludes()

    @Input
    abstract Property<Object> getPackageAnalysis()

    @Input
    abstract Property<Object> getIncludeReferencedProjects()

    @Input
    abstract Property<Object> getIncludeEmptyDirectories()

    @Input
    abstract MapProperty<String, Object> getProperties()

    @Optional
    @Input
    abstract Property<Object> getMinClientVersion()

    @Optional
    @Input
    abstract Property<Object> getMsBuildVersion()

    NuGetPack() {
        super('pack')
        // Force always execute
        outputs.upToDateWhen { false }
        destinationDir.convention(
                project.layout.buildDirectory.dir('distributions').get()
        )
        generateSymbols.convention(false)
        tool.convention(false)
        build.convention(false)
        defaultExcludes.convention(true)
        packageAnalysis.convention(true)
        includeReferencedProjects.convention(false)
        includeEmptyDirectories.convention(true)
        properties.convention([:])

        project.afterEvaluate {
            def spec = getNuspec()
            def specSources = spec?.files?.file?.collect { it.@src.text() }
            if (specSources && specSources.any()) {
                project.tasks.matching { matchingTask ->
                    matchingTask.class.name.startsWith('com.ullink.Msbuild') &&
                            matchingTask.projects.values().any { specSources.contains it.properties.TargetPath }
                }.each {
                    dependsOn it
                }
            }
        }
    }


    @Override
    void exec() {
        args getNuspecOrCsproj()
        def spec = getNuspec()

        def destDir = getDestinationDir().getAsFile().get()
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        args '-OutputDirectory', destDir

        if (basePath.isPresent()) args '-BasePath', basePath.get()

        def version = getFinalPackageVersion(spec)
        if (version) args '-Version', version

        if (exclude.isPresent()) args '-Exclude', exclude.get()
        if (generateSymbols.isPresent()) args '-Symbols'
        if (tool.isPresent()) args '-Tool'
        if (build.isPresent()) args '-Build'
        if (!defaultExcludes.isPresent()) args '-NoDefaultExcludes'
        if (!packageAnalysis.isPresent()) args '-NoPackageAnalysis'
        if (includeReferencedProjects.isPresent()) args '-IncludeReferencedProjects'
        if (!includeEmptyDirectories.isPresent()) args '-ExcludeEmptyDirectories'
        if (properties.isPresent()) args '-Properties', properties.get().collect { k, v -> "$k=$v" }.join(';')
        if (minClientVersion.isPresent()) args '-MinClientVersion', minClientVersion.get()
        final String _msBuildVersion = msBuildVersion.isPresent() ? msBuildVersion.get() :
                GradleHelper.getPropertyFromTask(project, 'version', 'msbuild')
        if (_msBuildVersion) args '-MsBuildVersion', _msBuildVersion
        super.exec()
    }

    void nuspec(Closure closure) {
        if (dependentNuGetSpec) {
            dependentNuGetSpec.nuspec closure
        } else {
            def nuGetSpec = project.tasks.register("nugetSpec_$name", NuGetSpec)
            nuGetSpec.with {
                group = BasePlugin.BUILD_GROUP
                description = "Generates nuspec file for task $name."
                nuspec closure
            }

            dependsOn nuGetSpec
        }
    }

    @Internal
    NuGetSpec getDependentNuGetSpec() {
        dependsOn.find { it instanceof NuGetSpec } as NuGetSpec
    }

    // Because Nuget pack handle csproj or nuspec file we should be able to use it in plugin
    @InputFile
    File getNuspecOrCsproj() {
        csprojPath.isPresent() ? csprojPath.get().asFile : getNuspecFile()
    }

    @Internal
    GPathResult getNuspec() {
        def nuspecFile = getNuspecFile()
        if (nuspecFile?.exists()) {
            return new XmlSlurper(false, false).parse(nuspecFile)
        }
        if (dependentNuGetSpec) {
            def generatedNuspec = dependentNuGetSpec.generateNuspec()
            if (generatedNuspec) {
                return new XmlSlurper(false, false).parseText(generatedNuspec)
            }
        }
        return null
    }

    File getNuspecFile() {
        nuspecFile ?: dependentNuGetSpec ? dependentNuGetSpec.nuspecFile : null as File
    }

    @OutputFile
    File getPackageFile() {
        def spec = getNuspec()
        def version = getFinalPackageVersion(spec)
        def id = spec?.metadata?.id?.toString() ?: getIdFromMsbuildTask()
        new File(getDestinationDir().getAsFile().get(), id + '.' + version + '.nupkg')
    }

    private String getFinalPackageVersion(def spec) {
        packageVersion.isPresent() ? packageVersion.get() : spec?.metadata?.version ?: project.version
    }

    @Internal
    String getIdFromMsbuildTask() {
        def isInputProject = { csprojPath.get().asFile.equals(it.projectFile) }
        def msbuildTask = project.tasks.find {
            it.class.name.startsWith('com.ullink.Msbuild') && it.projects.values().any(isInputProject)
        }
        if (msbuildTask != null) {
            return FilenameUtils.removeExtension(msbuildTask.projects.values().find(isInputProject).dotnetAssemblyFile.name)
        }
        return null
    }

}