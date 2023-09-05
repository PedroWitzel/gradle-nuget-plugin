package com.ullink

import org.junit.Before

import static org.junit.Assert.*
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class NuGetPluginTest {

    private Project project

    @Before
    void init() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'nuget'
    }

    @Test
    void nugetPluginAddsNuGetTasksToProject() {
        assertTrue(project.tasks.nugetPack instanceof NuGetPack)
        assertTrue(project.tasks.nugetPush instanceof NuGetPush)
        assertTrue(project.tasks.nugetSpec instanceof NuGetSpec)
        assertTrue(project.tasks.nugetSources instanceof NuGetSources)
    }

    @Test
    public void nugetPackWorks() {
        def task = project.tasks.named('nugetPack').get()
        def temporaryDir = task.temporaryDir
        File nuspec = new File(temporaryDir, 'foo.nuspec')
        nuspec.text = '''<?xml version='1.0'?>
<package xmlns='http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd'>
  <metadata>
    <id>foo</id>
    <authors>Nobody</authors>
    <version>1.2.3</version>
    <description>fooDescription</description>
  </metadata>
  <files>
    <file src='foo.txt' />
  </files>
</package>'''

        File fooFile = new File(temporaryDir, 'foo.txt')
        fooFile.text = "Bar"

        project.nugetPack {
            basePath = temporaryDir
            nuspecFile = nuspec
        }

        task.exec()
        assertTrue(task.packageFile.exists())
    }

    @Test
    void nugetPackSpecifyVersion() {
        def task = project.tasks.named('nugetPack').get()
        def temporaryDir = task.temporaryDir
        File nuspec = new File(temporaryDir, 'bar.nuspec')
        nuspec.text = '''<?xml version='1.0'?>
<package xmlns='http://schemas.microsoft.com/packaging/2011/08/nuspec.xsd'>
  <metadata>
    <id>bar</id>
    <authors>Nobody</authors>
    <version>1.2.3</version>
    <description>fooDescription</description>
  </metadata>
  <files>
    <file src='bar.txt' />
  </files>
</package>'''

        File fooFile = new File(temporaryDir, 'bar.txt')
        fooFile.text = 'Bar'

        project.nugetPack {
            basePath = temporaryDir
            nuspecFile = nuspec
            packageVersion = '100.200.300'
        }

        task.exec()
        def packageFile = task.packageFile
        assertEquals('bar.100.200.300.nupkg', packageFile.name)
        assertTrue(packageFile.exists())
    }
}
