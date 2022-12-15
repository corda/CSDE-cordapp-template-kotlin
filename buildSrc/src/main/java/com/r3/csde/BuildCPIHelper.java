package com.r3.csde;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.LinkedList;

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
//            Runtime.getRuntime().exec("echo tesing exec");
        }
    }
}
