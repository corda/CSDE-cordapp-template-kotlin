package com.r3.csde;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Manages Bringing corda up, testing for liveness and takign corda down
 */

public class CordaLifeCycleHelper {

    ProjectContext pc;

    public CordaLifeCycleHelper(ProjectContext _pc) {
        pc = _pc;
    }


    public void startCorda() throws IOException {
        PrintStream pidStore = new PrintStream(new FileOutputStream(pc.cordaPidCache));
        File combinedWorkerJar = pc.project.getConfigurations().getByName("combinedWorker").getSingleFile();

        new ProcessBuilder(
                "docker",
                "run", "-d", "--rm",
                "-p", "5432:5432",
                "--name", pc.dbContainerName,
                "-e", "POSTGRES_DB=cordacluster",
                "-e", "POSTGRES_USER=postgres",
                "-e", "POSTGRES_PASSWORD=password",
                "postgres:latest").start();
        ProjectUtils.rpcWait(10000);

        ProcessBuilder procBuild = new ProcessBuilder(pc.javaBinDir + "/java",
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
                "-Dco.paralleluniverse.fibers.verifyInstrumentation=true",
                "-jar",
                combinedWorkerJar.toString(),
                "--instanceId=0",
                "-mbus.busType=DATABASE",
                "-spassphrase=password",
                "-ssalt=salt",
                "-spassphrase=password",
                "-ssalt=salt",
                "-ddatabase.user=user",
                "-ddatabase.pass=password",
                "-ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster",
                "-ddatabase.jdbc.directory="+pc.JDBCDir);


        procBuild.redirectErrorStream(true);
        Process proc = procBuild.start();
        pidStore.print(proc.pid());
        pc.out.println("Corda Process-id="+proc.pid());
    }


    public void stopCorda() throws IOException, NoPidFile {
        File cordaPIDFile = new File(pc.cordaPidCache);
        if(cordaPIDFile.exists()) {
            Scanner sc = new Scanner(cordaPIDFile);
            long pid = sc.nextLong();
            pc.out.println("pid to kill=" + pid);

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("Powershell", "-Command", "Stop-Process", "-Id", Long.toString(pid), "-PassThru").start();
            } else {
                new ProcessBuilder("kill", "-9", Long.toString(pid)).start();
            }

            Process proc = new ProcessBuilder("docker", "stop", pc.dbContainerName).start();

            cordaPIDFile.delete();
        }
        else {
            throw new NoPidFile("Cannot stop the Combined worker\nCached process ID file " + pc.cordaPidCache + " missing.\nWas the combined worker not started?");
        }
    }


}
