package com.ullink

import com.ullink.packagesparser.NugetParser
import com.ullink.packagesparser.PackageReferenceParser
import com.ullink.packagesparser.PackagesConfigParser
import com.ullink.packagesparser.ProjectJsonParser
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import groovy.xml.slurpersupport.GPathResult
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile

abstract class NuGetSpec extends Exec {

    @Internal
    def nuspec

    void exec() {
        generateNuspecFile()
    }

    @Internal
    RegularFile getTempNuspecFile() {
        project.layout.projectDirectory.file(new File(temporaryDir, project.name + '.nuspec').path)
    }

    @OutputFile
    abstract RegularFileProperty getNuspecFile()

    NuGetSpec() {
        nuspecFile.convention(getTempNuspecFile())
    }

    void generateNuspecFile() {
        def nuspecXml = generateNuspec()
        if (nuspecXml) {
            getNuspecFile().write(nuspecXml, 'utf-8')
        }
    }

    String generateNuspec() {
        if (nuspec) {
            def sw = new StringWriter()
            new MarkupBuilder(sw).with {
                def visitor
                visitor = { entry ->
                    switch (entry) {
                        case Closure:
                            entry.delegate = delegate
                            entry.call()
                            break
                        case Map.Entry:
                            "$entry.key" { visitor entry.value }
                            break
                        case Map:
                        case Collection:
                            entry.collect(visitor)
                            break
                        default:
                            mkp.yield(entry)
                            break
                    }
                }
                'package'(xmlns: 'http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd') {
                    visitor nuspec
                }
            }
            return supplementDefaultValueOnNuspec(sw.toString())
        } else {
            return null
        }
    }

    String supplementDefaultValueOnNuspec(String nuspecString) {
        boolean msbuildTaskExists = true
        Task msbuildTask
        try {
            msbuildTask = project.tasks.named('msbuild').get()
        } catch (ignored) {
            msbuildTaskExists = false
            msbuildTask = null
        }

        final String packageConfigFileName = 'packages.config'
        final String projectJsonFileName = 'project.json'

        GPathResult root = new groovy.xml.XmlSlurper(false, false).parseText(nuspecString)

        def defaultMetadata = []
        def setDefaultMetadata = { String node, value ->
            if (root.metadata[node].isEmpty()) {
                defaultMetadata.add({ delegate."$node" value })
            }
        }

        setDefaultMetadata('id', project.name)
        setDefaultMetadata('version', project.version)
        setDefaultMetadata('description', project.description ? project.description : project.name)

        def appendAndCreateParentIfNeeded = {
            String parentNodeName, List children ->
                if (!children.isEmpty()) {
                    if (root."$parentNodeName".isEmpty()) {
                        root << { "$parentNodeName" children }
                    } else {
                        root."$parentNodeName" << children
                    }
                }
        }

        if (msbuildTaskExists) {
            project.logger.debug('Msbuild plugin detected')
            if (msbuildTask.parseProject) {
                project.logger.debug('Add defaults from Msbuild plugin')
                def mainProject = msbuildTask.mainProject

                if (root.files.file.isEmpty()) {
                    project.logger.debug('No files already defined in the NuGet spec, will add the ones from the msbuild task.')
                    def defaultFiles = []
                    msbuildTask.mainProject.dotnetArtifacts.each { artifact ->
                        def fwkFolderVersion = mainProject.properties.TargetFrameworkVersion.toString().replace('v', '').replace('.', '')
                        defaultFiles.add({ file(src: artifact.toString(), target: 'lib/net' + fwkFolderVersion) })
                    }
                    appendAndCreateParentIfNeeded('files', defaultFiles)
                }
                def dependencies = []
                dependencies.addAll getDependencies(mainProject, packageConfigFileName, new PackagesConfigParser())
                dependencies.addAll getDependencies(mainProject, projectJsonFileName, new ProjectJsonParser())
                dependencies.addAll getDependencies(mainProject, mainProject.properties.MSBuildProjectFile.toString(), new PackageReferenceParser())

                if (!dependencies.isEmpty()) setDefaultMetadata('dependencies', dependencies)

            } else {
                project.logger.debug("Msbuild plugin is configured with parseProject=false, no defaults added")
            }
        }

        appendAndCreateParentIfNeeded('metadata', defaultMetadata)

        project.logger.info("Generated NuGetSpec file with ${root.files.file.size()} files " +
                "and ${root.dependencies.dependecy.size()} dependencies")
        XmlUtil.serialize(root)
    }

    Collection getDependencies(def mainProject, String fileName, NugetParser parser) {
        def file = new File(
                new File(mainProject.projectFile).parentFile,
                fileName)
        if (file.exists()) {
            project.logger.debug("Adding dependencies from ${fileName}")
            return parser.getDependencies(file)
        }
        return []
    }
}
