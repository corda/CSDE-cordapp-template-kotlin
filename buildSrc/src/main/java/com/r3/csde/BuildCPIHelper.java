package com.r3.csde;

import java.io.*;
import java.util.LinkedList;

public class BuildCPIHelper {

    public ProjectContext pc;
    public ProjectUtils utils ;
public BuildCPIHelper(ProjectContext _pc) {
    pc = _pc;
    utils = new ProjectUtils(pc);
}

//todo: delete test code
    public void testExec() throws IOException, InterruptedException {
        Process command = Runtime.getRuntime().exec("echo testing exec \n next line");
        BufferedReader op = new BufferedReader(new InputStreamReader(command.getInputStream()));
        command.waitFor();
        pc.out.println(op.readLine());


}

    public void createGroupPolicy() throws IOException {

        File groupPolicyFile = new File(String.format("%s/GroupPolicy.json", pc.devEnvWorkspace));
        File devnetFile = new File(String.format("%s/config/dev-net.json", pc.project.getRootDir()));

        if (!groupPolicyFile.exists() || groupPolicyFile.lastModified() < devnetFile.lastModified()) {

            pc.out.println("createGroupPolicy: Creating a GroupPolicy");

            LinkedList<String> configX500Ids = utils.getConfigX500Ids(pc.X500ConfigFile);
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

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            // todo add exception catching
            FileWriter fileWriter = new FileWriter(groupPolicyFile);
            String line;
            while (( line = reader.readLine()) != null){
                fileWriter.write(line + "\n");
            }
            fileWriter.close();

        } else {
            pc.out.println("createPolicyTask: everything up to date; nothing to do.");
        }

    }

    public void createKeyStore() throws IOException, InterruptedException {

        File keystoreFile = new File(pc.keystoreFName);
        if(!keystoreFile.exists()) {
            pc.out.println("createKeystore: Create a keystore");

            generateKeyPair();
            addDefaultSigningKey();
            exportCert();

        } else {
            pc.out.println("createKeystore:  keystore already created; nothing to do.");
        }

    }

    private void generateKeyPair() throws IOException, InterruptedException {

        LinkedList<String> cmdArray = new LinkedList<>();

        cmdArray.add(pc.javaBinDir + "/keytool");
        cmdArray.add("-genkeypair");
        cmdArray.add("-alias");
        cmdArray.add(pc.keystoreAlias);
        cmdArray.add("-keystore");
        cmdArray.add(pc.keystoreFName);
        cmdArray.add("-storepass");
        cmdArray.add("keystore password");
        cmdArray.add("-dname");
        cmdArray.add("CN=CPI Example - My Signing Key, O=CorpOrgCorp, L=London, C=GB");
        cmdArray.add("-keyalg");
        cmdArray.add("RSA");
        cmdArray.add("-storetype");
        cmdArray.add("pkcs12");
        cmdArray.add("-validity");
        cmdArray.add("4000");

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

    }

    private void addDefaultSigningKey() throws IOException, InterruptedException {

        LinkedList<String> cmdArray = new LinkedList<>();

        cmdArray.add(pc.javaBinDir + "/keytool");
        cmdArray.add("-importcert");
        cmdArray.add("-keystore");
        cmdArray.add(pc.keystoreFName);
        cmdArray.add("-storepass");
        cmdArray.add("keystore password");
        cmdArray.add("-noprompt");
        cmdArray.add("-alias");
        cmdArray.add(pc.signingCertAlias);
        cmdArray.add("-file");
        cmdArray.add(pc.signingCertFName);

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();
    }

    private void exportCert() throws IOException, InterruptedException {

        LinkedList<String> cmdArray = new LinkedList<>();

        cmdArray.add(pc.javaBinDir + "/keytool");
        cmdArray.add("-exportcert");
        cmdArray.add("-rfc");
        cmdArray.add("-alias");
        cmdArray.add(pc.keystoreAlias);
        cmdArray.add("-keystore");
        cmdArray.add(pc.keystoreFName);
        cmdArray.add("-storepass");
        cmdArray.add("keystore password");
        cmdArray.add("-file");
        cmdArray.add(pc.keystoreCertFName);

        ProcessBuilder pb = new ProcessBuilder(cmdArray);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        proc.waitFor();

    }



}
