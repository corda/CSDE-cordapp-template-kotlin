package net.corda.plugins.csde

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

class CsdePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def extension = project.extensions.create("csde", CsdeExtension)

        project.tasks.register("installCordaLite", Copy) {
            from project.configurations.combinedWorker
            into project.cordaBinDir
        }
    }
}
