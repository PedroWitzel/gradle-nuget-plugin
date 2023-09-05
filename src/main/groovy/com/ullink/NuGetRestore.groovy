package com.ullink

import com.ullink.util.GradleHelper
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

abstract class NuGetRestore extends BaseNuGet {

    @Optional
    @InputFile
    abstract RegularFileProperty getSolutionFile()

    @Optional
    @InputFile
    abstract RegularFileProperty getPackagesConfigFile()

    @Input
    abstract SetProperty<Object> getSources()

    @Input
    abstract Property<Boolean> getNoCache()

    @Optional
    @InputFile
    abstract RegularFileProperty getConfigFile()

    @Input
    abstract Property<Boolean> getRequireConsent()

    @Optional
    @InputDirectory
    abstract DirectoryProperty getSolutionDirectory()

    @Input
    abstract Property<Boolean> getDisableParallelProcessing()

    @Optional
    @Input
    abstract Property<String> getMsBuildVersion()

    @Optional
    @InputDirectory
    abstract DirectoryProperty getPackagesDirectory()

    NuGetRestore() {
        super('restore')
        noCache.convention(false)
        requireConsent.convention(false)
        disableParallelProcessing.convention(false)

        // Force always execute
        outputs.upToDateWhen { false }
    }

    /**
     * @Deprecated Only provided for backward compatibility. Uses 'sources' instead
     */
    @Deprecated
    def setSource(String source) {
        sources.get().clear()
        sources.get().add(source)
    }

    @Override
    void exec() {
        if (packagesConfigFile.isPresent()) args packagesConfigFile.get()
        if (solutionFile.isPresent()) args solutionFile.get()

        if (sources.isPresent() && !sources.get().isEmpty()) args '-Source', sources.get().join(';')
        if (noCache.isPresent()) args '-NoCache'
        if (configFile.isPresent()) args '-ConfigFile', configFile.get()
        if (requireConsent.isPresent()) args '-RequireConsent'
        if (packagesDirectory.isPresent()) args '-PackagesDirectory', packagesDirectory.get()
        if (solutionDirectory.isPresent()) args '-SolutionDirectory', solutionDirectory.get()
        if (disableParallelProcessing.isPresent()) args '-DisableParallelProcessing'

        final String _msBuildVersion = msBuildVersion.isPresent() ? msBuildVersion.get() :
                GradleHelper.getPropertyFromTask(project, 'version', 'msbuild')
        if (_msBuildVersion) args '-MsBuildVersion', _msBuildVersion

        project.logger.info "Restoring NuGet packages " +
                (sources.isPresent() ? "from ${sources.get()}" : '') +
                (packagesConfigFile.isPresent() ? "for packages.config (${packagesConfigFile.get()}" : '') +
                (solutionFile.isPresent() ? "for solution file (${solutionFile.get()}" : '')
        super.exec()
    }

    @OutputDirectory
    File getPackagesFolder() {
        // https://docs.nuget.org/consume/command-line-reference#restore-command
        // If -PackagesDirectory <packagesDirectory> is specified, <packagesDirectory> is used as the packages directory.
        if (packagesDirectory.isPresent()) {
            return packagesDirectory.get().asFile
        }

        // If -SolutionDirectory <solutionDirectory> is specified, <solutionDirectory>\packages is used as the packages directory.
        // SolutionFile can also be provided.
        // Otherwise use '.\packages'
        def solutionDir = solutionFile.isPresent() ?
                project.file(solutionFile.get().asFile.getParent()) :
                solutionDirectory.get().asFile
        return new File(solutionDir ? solutionDir.toString() : '.', 'packages')
    }
}
