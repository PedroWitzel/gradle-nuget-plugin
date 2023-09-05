package com.ullink.packagesparser

import groovy.xml.XmlParser

class PackagesConfigParser implements NugetParser {

    @Override
    Collection getDependencies(File file) {
        def defaultDependencies = []
        new XmlParser().parse(file)
                .package
                .findAll { !it.@developmentDependency.toString().toBoolean() }
                .each { packageElement ->
                    defaultDependencies.add({
                        dependency(id: packageElement.@id, version: getVersion(packageElement))
                    })
                }
        return defaultDependencies
    }

    static String getVersion(Object element) {
        element.@allowedVersions ?: element.@version
    }
}
