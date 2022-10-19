package com.r3.csde;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class NewProjInit extends DefaultTask {
    @TaskAction
    public void run() {
        System.out.println("Pretend newProjInit task");
    }
}
