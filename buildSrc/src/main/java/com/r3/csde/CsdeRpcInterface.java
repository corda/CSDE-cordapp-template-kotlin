package com.r3.csde;

import kong.unirest.json.JSONArray;
import org.gradle.api.Project;
import net.corda.v5.base.types.MemberX500Name;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.jetbrains.annotations.NotNull;

import javax.naming.ConfigurationException;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import static java.lang.Thread.sleep;

public class CsdeRpcInterface {
    private Project project;
    private String baseURL = "https://localhost:8888";
    private String rpcUser = "admin";
    private String rpcPasswd = "admin";
    private String workspaceDir = "workspace";
    static private int retryWaitMs = 1000;
    static PrintStream out = System.out;
    static private String CPIUploadStatusBaseName = "CPIFileStatus.json";
    static private String CPIUploadStatusFName;
    static private String X500ConfigFile = "config/dev-net.json";
    static private String javaBinDir;
    static private String cordaPidCache = "CordaPIDCache.dat";
    static private String dbContainerName;
    private String JDBCDir;
    private String combinedWorkerBinRe;
    private Map<String, String> notaryRepresentatives = null;

    public CsdeRpcInterface() {
    }

    public CsdeRpcInterface (Project inProject,
                             String inBaseUrl,
                             String inRpcUser,
                             String inRpcPasswd,
                             String inWorkspaceDir,
                             String inJavaBinDir,
                             String inDbContainerName,
                             String inJDBCDir,
                             String inCordaPidCache
    ) {
        project = inProject;
        baseURL = inBaseUrl;
        rpcUser = inRpcUser;
        rpcPasswd = inRpcPasswd;
        workspaceDir = inWorkspaceDir;
        javaBinDir = inJavaBinDir;
        cordaPidCache = inCordaPidCache;
        dbContainerName = inDbContainerName;
        JDBCDir = inJDBCDir;
        CPIUploadStatusFName = workspaceDir +"/"+ CPIUploadStatusBaseName;

    }


    static private void rpcWait(int millis) {
        try {
            sleep(millis);
        }
        catch(InterruptedException e) {
            throw new UnsupportedOperationException("Interrupts not supported.", e);
        }
    }

    static private void rpcWait() {
        rpcWait(retryWaitMs);
    }

    public LinkedList<String> getConfigX500Ids() throws IOException {
        LinkedList<String> x500Ids = new LinkedList<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        FileInputStream in = new FileInputStream(X500ConfigFile);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);
        for( com.fasterxml.jackson.databind.JsonNode identity:  jsonNode.get("identities")) {
            x500Ids.add(jsonNodeToString(identity));
        }
        return x500Ids;
    }

    // KV pairs of representative x500 name and corresponding notary service x500 name
    public Map<String, String> getNotaryRepresentatives() throws IOException, ConfigurationException {
        if (notaryRepresentatives == null) {
            notaryRepresentatives = new HashMap<>();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            FileInputStream in = new FileInputStream(X500ConfigFile);
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);

            List<String> identities = getConfigX500Ids();

            for (com.fasterxml.jackson.databind.JsonNode notary : jsonNode.get("notaries")) {

                String svcX500Id = jsonNodeToString(notary.get("serviceX500Name"));

                com.fasterxml.jackson.databind.JsonNode repsForThisService = notary.get("representatives");

                if (repsForThisService.isEmpty()) {
                    throw new ConfigurationException(
                            "Notary service \"" + svcX500Id + "\" must have at least one representative.");
                } else if (repsForThisService.size() > 1) {
                    // Temporary restriction while the MGM only supports a 1-1 association
                    throw new ConfigurationException(
                            "Notary service \"" + svcX500Id + "\" can only have a single representative at this time.");
                }

                for (com.fasterxml.jackson.databind.JsonNode representative : repsForThisService) {

                    String repAsString = jsonNodeToString(representative);

                    if (identities.contains(repAsString)) {
                        notaryRepresentatives.put(repAsString, svcX500Id);
                    } else {
                        throw new ConfigurationException(
                                "Notary representative \"" + repAsString + "\" is not a valid identity");
                    }
                }
            }
        }

        return notaryRepresentatives;
    }

    static public String getLastCPIUploadChkSum(@NotNull String CPIUploadStatusFName) throws IOException, NullPointerException {

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        FileInputStream in = new FileInputStream(CPIUploadStatusFName);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);


        String checksum = jsonNode.get("cpiFileChecksum").toString();
        if(checksum == null || checksum.equals("null")) {
            throw new NullPointerException("Missing cpiFileChecksum in file " + CPIUploadStatusFName+ " with contents:" + jsonNode);
        }
        return checksum;
    }


    public void reportError(@NotNull kong.unirest.HttpResponse<kong.unirest.JsonNode> response) throws CsdeException {

        out.println("*** *** ***");
        out.println("Unexpected response from Corda");
        out.println("Status="+ response.getStatus());
        out.println("*** Headers ***\n"+ response.getHeaders());
        out.println("*** Body ***\n"+ response.getBody());
        out.println("*** *** ***");
        throw new CsdeException("Error: unexpected response from Corda.");
    }

    public void downloadFile(String url, String targetPath) {
        Unirest.get(url)
                .asFile(targetPath)
                .getBody();
    }

    public kong.unirest.HttpResponse<kong.unirest.JsonNode> getVNodeInfo() {
        Unirest.config().verifySsl(false);
        return Unirest.get(baseURL + "/api/v1/virtualnode/")
                .basicAuth(rpcUser, rpcPasswd)
                .asJson();
    }

    public void listVNodesVerbose() {
        kong.unirest.HttpResponse<kong.unirest.JsonNode> vnodeResponse = getVNodeInfo();
        out.println("VNodes:\n" + vnodeResponse.getBody().toPrettyString());
    }

    // X500Name, shorthash, cpiname
    public void listVNodes() {
        kong.unirest.HttpResponse<kong.unirest.JsonNode> vnodeResponse = getVNodeInfo();

        kong.unirest.json.JSONArray virtualNodesJson = (JSONArray) vnodeResponse.getBody().getObject().get("virtualNodes");
        out.println("X500 Name\tHolding identity short hash\tCPI Name");
        for(Object o: virtualNodesJson){
            if(o instanceof kong.unirest.json.JSONObject) {
                kong.unirest.json.JSONObject idObj = ((kong.unirest.json.JSONObject) o).getJSONObject("holdingIdentity");
                kong.unirest.json.JSONObject cpiObj = ((kong.unirest.json.JSONObject) o).getJSONObject("cpiIdentifier");
                out.print("\"" + idObj.get("x500Name") + "\"");
                out.print("\t\"" + idObj.get("shortHash") + "\"");
                out.println("\t\"" + cpiObj.get("cpiName") + "\"");
            }
        }
    }

    public kong.unirest.HttpResponse<kong.unirest.JsonNode> getCpiInfo() {
        Unirest.config().verifySsl(false);
        return Unirest.get(baseURL + "/api/v1/cpi/")
                .basicAuth(rpcUser, rpcPasswd)
                .asJson();

    }

    public void listCPIs() {
        kong.unirest.HttpResponse<kong.unirest.JsonNode> cpiResponse  = getCpiInfo();
        kong.unirest.json.JSONArray jArray = (JSONArray) cpiResponse.getBody().getObject().get("cpis");

        for(Object o: jArray){
            if(o instanceof kong.unirest.json.JSONObject) {
                kong.unirest.json.JSONObject idObj = ((kong.unirest.json.JSONObject) o).getJSONObject("id");
                out.print("cpiName=" + idObj.get("cpiName"));
                out.println(", cpiVersion=" + idObj.get("cpiVersion"));
            }
        }
    }

    public void uploadCertificate(String certAlias, String certFName) {
        Unirest.config().verifySsl(false);
        kong.unirest.HttpResponse<kong.unirest.JsonNode> uploadResponse = Unirest.put(baseURL + "/api/v1/certificates/cluster/code-signer")
                .field("alias", certAlias)
                .field("certificate", new File(certFName))
                .basicAuth(rpcUser, rpcPasswd)
                .asJson();
        out.println("Certificate/key upload, alias "+certAlias+" certificate/key file "+certFName);
        out.println(uploadResponse.getBody().toPrettyString());
    }

    public void forceuploadCPI(String cpiFName) throws FileNotFoundException, CsdeException {
        Unirest.config().verifySsl(false);
        kong.unirest.HttpResponse<kong.unirest.JsonNode> jsonResponse = Unirest.post(baseURL + "/api/v1/maintenance/virtualnode/forcecpiupload/")
                .field("upload", new File(cpiFName))
                .basicAuth(rpcUser, rpcPasswd)
                .asJson();

        if(jsonResponse.getStatus() == 200) {
            String id = (String) jsonResponse.getBody().getObject().get("id");
            out.println("get id:\n" +id);
            kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = uploadStatus(id);

            if (statusResponse.getStatus() == 200) {
                PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(CPIUploadStatusFName));
                cpiUploadStatus.print(statusResponse.getBody());
                out.println("Caching CPI file upload status:\n" + statusResponse.getBody());
            } else {
                reportError(statusResponse);
            }
        }
        else {
            reportError(jsonResponse);
        }
    }

    private boolean uploadStatusRetry(kong.unirest.HttpResponse<kong.unirest.JsonNode> response) {
        int status = response.getStatus();
        kong.unirest.JsonNode body = response.getBody();
        // Do not retry on success
        if(status == 200) {
            // Keep retrying until we get "OK" may move through "Validateing upload", "Persisting CPI"
            return !(body.getObject().get("status").equals("OK"));
        }
        else if (status == 400){
            JSONObject details = response.getBody().getObject().getJSONObject("details");
            if( details != null ){
                String code = (String) details.getString("code");
                return !code.equals("BAD_REQUEST");
            }
            else {
                // 400 otherwise means some transient problem
                return true;
            }
        }
        return false;
    }

    public kong.unirest.HttpResponse<kong.unirest.JsonNode> uploadStatus(String requestId) {
        kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = null;
        do {
            rpcWait(1000);
            statusResponse = Unirest
                    .get(baseURL + "/api/v1/cpi/status/" + requestId + "/")
                    .basicAuth(rpcUser, rpcPasswd)
                    .asJson();
            out.println("Upload status="+statusResponse.getStatus()+", status query response:\n"+statusResponse.getBody().toPrettyString());
        }
        while(uploadStatusRetry(statusResponse));

        return statusResponse;
    }

    public void deployCPI(String cpiFName, String cpiName, String cpiVersion) throws FileNotFoundException, CsdeException {
        Unirest.config().verifySsl(false);

        kong.unirest.HttpResponse<kong.unirest.JsonNode> cpiResponse  = getCpiInfo();
        kong.unirest.json.JSONArray jArray = (JSONArray) cpiResponse.getBody().getObject().get("cpis");


        int matches = 0;
        for(Object o: jArray.toList() ) {
            if(o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("id");
                if((idObj.get("cpiName").toString().equals(cpiName)
                        && idObj.get("cpiVersion").toString().equals(cpiVersion))) {
                    matches++;
                }
            }
        }
        out.println("Matching CPIS="+matches);


        if(matches == 0) {
            kong.unirest.HttpResponse<kong.unirest.JsonNode> uploadResponse = Unirest.post(baseURL + "/api/v1/cpi/")
                    .field("upload", new File(cpiFName))
                    .basicAuth(rpcUser, rpcPasswd)
                    .asJson();

            kong.unirest.JsonNode body = uploadResponse.getBody();

            int status = uploadResponse.getStatus();

            out.println("Upload Status:" + status);
            out.println("Pretty print the body\n" + body.toPrettyString());

            // We expect the id field to be a string.
            if (status == 200) {
                String id = (String) body.getObject().get("id");
                out.println("get id:\n" + id);

                kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = uploadStatus(id);
                if (statusResponse.getStatus() == 200) {
                    PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(CPIUploadStatusFName));
                    cpiUploadStatus.print(statusResponse.getBody());
                    out.println("Caching CPI file upload status:\n" + statusResponse.getBody());
                } else {
                    reportError(statusResponse);
                }
            } else {
                reportError(uploadResponse);
            }
        }
        else {
            out.println("CPI already uploaded doing a 'force' upload.");
            forceuploadCPI(cpiFName);
        }
    }

    public void createAndRegVNodes() throws IOException, CsdeException, ConfigurationException{
        Unirest.config().verifySsl(false);
        String cpiCheckSum = getLastCPIUploadChkSum( CPIUploadStatusFName );

        LinkedList<String> x500Ids = getConfigX500Ids();
        // Map of X500 name to short hash
        Map<String, String> OKHoldingX500AndShortIds = new HashMap<>();

        // For each identity check that it already exists.
        Set<MemberX500Name> existingX500 = new HashSet<>();
        kong.unirest.HttpResponse<kong.unirest.JsonNode> vnodeListResponse = getVNodeInfo();

        kong.unirest.json.JSONArray virtualNodesJson = (JSONArray) vnodeListResponse.getBody().getObject().get("virtualNodes");
        for(Object o: virtualNodesJson){
            if(o instanceof kong.unirest.json.JSONObject) {
                kong.unirest.json.JSONObject idObj = ((kong.unirest.json.JSONObject) o).getJSONObject("holdingIdentity");
                String x500id = (String) idObj.get("x500Name");
                existingX500.add(MemberX500Name.parse( x500id) );
            }
        }

        // Create the VNodes
        for(String x500id: x500Ids) {
            if(!existingX500.contains(MemberX500Name.parse(x500id) )) {
                out.println("Creating VNode for x500id=\"" + x500id + "\" cpi checksum=" + cpiCheckSum);
                kong.unirest.HttpResponse<kong.unirest.JsonNode> jsonNode = Unirest.post(baseURL + "/api/v1/virtualnode")
                        .body("{ \"request\" : { \"cpiFileChecksum\": " + cpiCheckSum + ", \"x500Name\": \"" + x500id + "\" } }")
                        .basicAuth(rpcUser, rpcPasswd)
                        .asJson();
                // Logging.

                // need to check this and report errors.
                // 200 - OK
                // 409 - Vnode already exists
                if (jsonNode.getStatus() != 409) {
                    if (jsonNode.getStatus() != 200) {
                        reportError(jsonNode);
                    } else {
                        JSONObject thing = jsonNode.getBody().getObject().getJSONObject("holdingIdentity");
                        String shortHash = (String) thing.get("shortHash");
                        OKHoldingX500AndShortIds.put(x500id, shortHash);
                    }
                }
            }
            else {
                out.println("Not creating a vnode for \"" + x500id + "\", vnode already exists.");
            }
        }

        // Register the VNodes
        for(String okId: OKHoldingX500AndShortIds.keySet()) {
            kong.unirest.HttpResponse<kong.unirest.JsonNode> vnodeResponse = Unirest
                    .post(baseURL + "/api/v1/membership/" +  OKHoldingX500AndShortIds.get(okId))
                    .body(getMemberRegistrationBody(okId))
                    .basicAuth(rpcUser, rpcPasswd)
                    .asJson();

            out.println("Vnode membership submission:\n" + vnodeResponse.getBody().toPrettyString());
        }

    }

    public void startCorda() throws IOException {
        PrintStream pidStore = new PrintStream(new FileOutputStream(cordaPidCache));
        File combinedWorkerJar = project.getConfigurations().getByName("combinedWorker").getSingleFile();

        new ProcessBuilder(
                "docker",
                "run", "-d", "--rm",
                "-p", "5432:5432",
                "--name", dbContainerName,
                "-e", "POSTGRES_DB=cordacluster",
                "-e", "POSTGRES_USER=postgres",
                "-e", "POSTGRES_PASSWORD=password",
                "postgres:latest").start();
        rpcWait(10000);

        ProcessBuilder procBuild = new ProcessBuilder(javaBinDir + "/java",
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
                "-ddatabase.jdbc.directory="+JDBCDir);


        procBuild.redirectErrorStream(true);
        Process proc = procBuild.start();
        pidStore.print(proc.pid());
        out.println("Corda Process-id="+proc.pid());
    }

    public void stopCorda() throws IOException, NoPidFile {
        File cordaPIDFile = new File(cordaPidCache);
        if(cordaPIDFile.exists()) {
            Scanner sc = new Scanner(cordaPIDFile);
            long pid = sc.nextLong();
            out.println("pid to kill=" + pid);

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                new ProcessBuilder("Powershell", "-Command", "Stop-Process", "-Id", Long.toString(pid), "-PassThru").start();
            } else {
                new ProcessBuilder("kill", "-9", Long.toString(pid)).start();
            }

            Process proc = new ProcessBuilder("docker", "stop", dbContainerName).start();

            cordaPIDFile.delete();
        }
        else {
            throw new NoPidFile("Cannot stop the Combined worker\nCached process ID file " + cordaPidCache + " missing.\nWas the combined worker not started?");
        }
    }

    // Helper to extract a string from a  JSON node and strip quotes
    private String jsonNodeToString(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        String jsonString = jsonNode.toString();
        return jsonString.substring(1, jsonString.length()-1);
    }

    private String getMemberRegistrationBody(String memberX500Name) throws ConfigurationException, IOException {
        Map<String, String> notaryReps = getNotaryRepresentatives();

        String context = "\"corda.key.scheme\" : \"CORDA.ECDSA.SECP256R1\"" + (
                notaryReps.containsKey(memberX500Name)
                ? ", \"corda.roles.0\" : \"notary\", " +
                        "\"corda.notary.service.name\" : \"" + notaryReps.get(memberX500Name) + "\", " +
                        // This will need revisiting in the long term when additional protocols are added, and will
                        // need to be specified in config. We will also need to review the hard-coded name once
                        // notary plugin selection logic is re-instated in CORE-7248.
                        "\"corda.notary.service.plugin\" : \"corda.notary.type.nonvalidating\""
                : ""
        );

        return "{ \"memberRegistrationRequest\": { \"action\": \"requestJoin\",  \"context\": { " + context + " } } }";
    }
}
