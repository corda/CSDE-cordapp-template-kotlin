package com.r3.csde;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class BuildCPIHelper {

    public ProjectContext pc;
    public ProjectUtils utils ;
public BuildCPIHelper(ProjectContext _pc) {
    pc = _pc;
    utils = new ProjectUtils(pc);
}

    public void testExec() throws IOException, InterruptedException {
        Process command = Runtime.getRuntime().exec("echo testing exec \n next line");
        BufferedReader op = new BufferedReader(new InputStreamReader(command.getInputStream()));
        command.waitFor();
        pc.out.println(op.readLine());


}

            // todo: Start here -> migrate gradle cpibuild tasks to Java

    public void createGroupPolicy() throws IOException {

        File groupPolicyFile = new File(String.format("%s/GroupPolicy.json", pc.devEnvWorkspace));
        File devnetFile = new File(String.format("%s/config/dev-net.json", pc.project.getRootDir()));

        if (!groupPolicyFile.exists() || groupPolicyFile.lastModified() < devnetFile.lastModified()) {
            LinkedList<String> configX500Ids = utils.getConfigX500Ids(pc.X500ConfigFile);
            pc.out.println("createGroupPolicy: Creating a GroupPolicy");

            LinkedList<String> cmdArray = new LinkedList<>();

            cmdArray.add(String.format("%s/java", pc.javaBinDir));
            cmdArray.add(String.format("-Dpf4j.pluginsDir=%s/plugins/", pc.cordaCliBinDir));
            cmdArray.add("-jar");
            cmdArray.add(String.format("%s/corda-cli.jar", pc.cordaCliBinDir));

            cmdArray.add("mgm");
            cmdArray.add("groupPolicy");
            for (String id : configX500Ids) {
                cmdArray.add("--name");
                cmdArray.add(id);
            }
            cmdArray.add("--endpoint-protocol=1");
            cmdArray.add("--endpoint=http://localhost:1080");

            // todo: remove this + tidy up rest.
            pc.out.println(cmdArray);

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            // todo add exception
            FileWriter fileWriter = new FileWriter(groupPolicyFile);
            String line;
            while (( line = reader.readLine()) != null){
                fileWriter.write(line + "\n");
            }
            fileWriter.close();
            // todo: START HERE -> check if Group policy generated works




        } else {
            pc.out.println("createPolicyTask: everything up to date; nothing to do.");
        }

    }
}
