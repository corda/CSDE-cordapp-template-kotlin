package net.corda.plugins.csde

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskAction

def fooThingPackager = tasks.register('fooThingTask') {
    println('fooThingTask output')
}

artifacts {
    archives fooThingPackager
}

class CsdePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def extension = project.extensions.create("csde", CsdeExtension)

        project.tasks.register("installCordaLite", Copy) {
            from project.configurations.combinedWorker
            into project.cordaBinDir
        }

        project.tasks.register("hello").doLast {
            println("Hello!")
        }
    }
}
/*
class MyTask extends DefaultTask {
    @TaskAction
    void run() {
        println ('Hello from MyTask')
    }
}

project.tasks.register('myTask', MyTask)
*/