package com.r3.csde;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import net.corda.v5.base.types.MemberX500Name;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.naming.ConfigurationException;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;
import static java.net.HttpURLConnection.*;

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
        return getLastCPIUploadChkSum(CPIUploadStatusFName, "");
    }

    static public String getLastCPIUploadChkSum(@NotNull String CPIUploadStatusFName,
                                                String uploadStatusQualifier) throws IOException, NullPointerException {

        String qualifiedCPIUploadStatusFName =
                CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json");

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        FileInputStream in = new FileInputStream(qualifiedCPIUploadStatusFName);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);

        String checksum = jsonNode.get("cpiFileChecksum").toString();
        if(checksum == null || checksum.equals("null")) {
            throw new NullPointerException("Missing cpiFileChecksum in file " +
                    qualifiedCPIUploadStatusFName + " with contents:" + jsonNode);
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

    Map<String, String> pollForVNodeShortHoldingHashIds(List<String> x500Ids, int retryCount, int coolDownMs ) throws CsdeException {
        HashMap<String, String> x500NameToShortHashes = new HashMap<>();
        Set<String> vnodesToCheck = new HashSet<String>(x500Ids);
        while(!vnodesToCheck.isEmpty() && retryCount-- > 0) {
            rpcWait(coolDownMs);
            kong.unirest.json.JSONArray virtualNodes = (JSONArray) getVNodeInfo().getBody().getObject().get("virtualNodes");
            Map<String, String> vnodesMap = new HashMap<String, String>();
            for (Object virtualNode : virtualNodes) {
                if (virtualNode instanceof JSONObject) {
                    JSONObject idObj = ((JSONObject) virtualNode).getJSONObject("holdingIdentity");
                    vnodesMap.put(idObj.get("x500Name").toString(), idObj.get("shortHash").toString());
                }
            }
            for(String x500Name: vnodesToCheck) {
                if(vnodesMap.containsKey(x500Name)) {
                    x500NameToShortHashes.put(x500Name, vnodesMap.get(x500Name));
                }
            }
            vnodesMap.keySet().forEach(vnodesToCheck::remove);
        }
        if(!vnodesToCheck.isEmpty()) {
            throw new CsdeException("VNode creation timed out. Not all expected vnodes were reported as created:" + vnodesToCheck.toString());
        }
        return x500NameToShortHashes;
    }

    private void pollForCompleteMembershipRegistration(Map<String, String> X500ToShortIdHash) throws CsdeException {
        HashSet<String> vnodesToCheck = new HashSet<String>(X500ToShortIdHash.keySet());
        LinkedList<String> approved = new LinkedList<String>();
        while (!vnodesToCheck.isEmpty()) {
            rpcWait(2000);
            approved.clear();
            for (String vnodeX500 : vnodesToCheck) {
                try {
                    out.println("Checking membership registration progress for v-node '" + vnodeX500 + "':");
                    kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = Unirest
                            .get(baseURL + "/api/v1/membership/" + X500ToShortIdHash.get(vnodeX500) + "/")
                            .basicAuth(rpcUser, rpcPasswd)
                            .asJson();
                    if (isMembershipRegComplete(statusResponse)) {
                        approved.add(vnodeX500);
                    }
                } catch (Exception e) {
                    throw new CsdeException("Error when registering V-Node '" + vnodeX500 + "'", e);
                }
            }
            approved.forEach(vnodesToCheck::remove);
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
        forceuploadCPI(cpiFName, "");
    }

    public void forceuploadCPI(String cpiFName, String uploadStatusQualifier) throws FileNotFoundException, CsdeException {
        Unirest.config().verifySsl(false);
        kong.unirest.HttpResponse<kong.unirest.JsonNode> jsonResponse = Unirest.post(baseURL + "/api/v1/maintenance/virtualnode/forcecpiupload/")
                .field("upload", new File(cpiFName))
                .basicAuth(rpcUser, rpcPasswd)
                .asJson();

        if(jsonResponse.getStatus() == HTTP_OK) {
            String id = (String) jsonResponse.getBody().getObject().get("id");
            out.println("get id:\n" +id);
            kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = uploadStatus(id);

            if (statusResponse.getStatus() == HTTP_OK) {
                PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(
                        CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json" )));
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
        if(status == HTTP_OK) {
            // Keep retrying until we get "OK" may move through "Validating upload", "Persisting CPI"
            return !(body.getObject().get("status").equals("OK"));
        }
        else if (status == HTTP_BAD_REQUEST){
            String bodyTitle = response.getBody().getObject().getString("title");
            return bodyTitle != null && bodyTitle.matches("No such requestId=[-0-9a-f]+");
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
        deployCPI(cpiFName, cpiName, cpiVersion, "");
    }

    public void deployCPI(String cpiFName,
                          String cpiName,
                          String cpiVersion,
                          String uploadStatusQualifier) throws FileNotFoundException, CsdeException {
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
            if (status == HTTP_OK) {
                String id = (String) body.getObject().get("id");
                out.println("get id:\n" + id);

                kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = uploadStatus(id);
                if (statusResponse.getStatus() == HTTP_OK) {
                    PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(
                            CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json" )));
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

    private boolean isMembershipRegComplete(kong.unirest.HttpResponse<kong.unirest.JsonNode> response) throws CsdeException {
        if(response.getStatus() == HTTP_OK) {
            kong.unirest.JsonNode responseBody = response.getBody();
            out.println(responseBody.toPrettyString());
            if(responseBody.getArray().length() > 0) {
                kong.unirest.json.JSONObject memRegStatusInfo = (kong.unirest.json.JSONObject) responseBody
                        .getArray()
                        .getJSONObject(0);
                String memRegStatus = memRegStatusInfo.get("registrationStatus").toString();
                if (memRegStatus.equals("DECLINED")) {
                    throw new CsdeException("V-Node membership registration declined by Corda");
                }
                return memRegStatus.equals("APPROVED");
            }
            else {
                return false;
            }
        }
        else {
            reportError(response);
        }
        return false;
    }


    public void createAndRegVNodes() throws IOException, CsdeException, ConfigurationException{
        Unirest.config().verifySsl(false);
        String appCpiCheckSum = getLastCPIUploadChkSum( CPIUploadStatusFName );
        String notaryCpiCheckSum = getLastCPIUploadChkSum( CPIUploadStatusFName, "-NotaryServer" );

        LinkedList<String> x500Ids = getConfigX500Ids();

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

        Map<String, CompletableFuture<HttpResponse<JsonNode>>> responses = new LinkedHashMap<>();

        // Create the VNodes
        for(String x500id: x500Ids) {
            if(!existingX500.contains(MemberX500Name.parse(x500id) )) {
                String cpiCheckSum = getNotaryRepresentatives().containsKey(x500id) ?  notaryCpiCheckSum : appCpiCheckSum;

                out.println("Creating VNode for x500id=\"" + x500id + "\" cpi checksum=" + cpiCheckSum);
                responses.put(x500id, Unirest
                        .post(baseURL + "/api/v1/virtualnode")
                        .body("{ \"request\" : { \"cpiFileChecksum\": " + cpiCheckSum + ", \"x500Name\": \"" + x500id + "\" } }")
                        .basicAuth(rpcUser, rpcPasswd)
                        .asJsonAsync()
                );
            }
            else {
                out.println("Not creating a vnode for \"" + x500id + "\", vnode already exists.");
            }
        }

        out.println("Waiting for VNode creation results...");

        for (Map.Entry<String, CompletableFuture<HttpResponse<JsonNode>>> response: responses.entrySet()) {
            try {
                HttpResponse<JsonNode> jsonNode = response.getValue().get();
                // need to check this and report errors.
                // 200/HTTP_OK - OK
                // 409/HTTP_CONFLICT - Vnode already exists
                // 500/HTTP_INTERNAL_ERROR
                //      - Can mean that the request timed out.
                //      - However, the cluster may still have created the V-node successfully, so we want to poll later.
                out.println("Vnode creation end point status:" + jsonNode.getStatus());
                switch(jsonNode.getStatus()) {
                    case HTTP_OK:               break;
                    case HTTP_CONFLICT:         break;
                    case HTTP_INTERNAL_ERROR:   break;
                    default:
                        reportError(jsonNode);
                }

            } catch (ExecutionException | InterruptedException e) {
                throw new CsdeException("Unexpected exception while waiting for response to " +
                        "membership submission for holding identity" + response.getKey());
            }
        }

        Map<String, String> OKHoldingX500AndShortIds = pollForVNodeShortHoldingHashIds(x500Ids, 60, 5000);

        // Register the VNodes
        responses.clear();

        for(String okId: OKHoldingX500AndShortIds.keySet()) {
            responses.put(okId, Unirest
                    .post(baseURL + "/api/v1/membership/" +  OKHoldingX500AndShortIds.get(okId))
                    .body(getMemberRegistrationBody(okId))
                    .basicAuth(rpcUser, rpcPasswd)
                    .asJsonAsync( response ->
                            out.println("Vnode membership submission for \"" + okId + "\"" +
                                    System.lineSeparator() + response.getBody().toPrettyString()))
            );
        }

        out.println("Vnode membership requests submitted, waiting for acknowledgement from MGM...");

        for (Map.Entry<String, CompletableFuture<HttpResponse<JsonNode>>> response: responses.entrySet()) {
            try {
                response.getValue().get();
            } catch (ExecutionException | InterruptedException e) {
                throw new CsdeException("Unexpected exception while waiting for response to " +
                        "membership submission for holding identity" + response.getKey());
            }
        }

        pollForCompleteMembershipRegistration(OKHoldingX500AndShortIds);
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
                        "\"corda.notary.service.plugin\" : \"net.corda.notary.NonValidatingNotary\""
                : ""
        );

        return "{ \"memberRegistrationRequest\": { \"action\": \"requestJoin\",  \"context\": { " + context + " } } }";
    }
}
