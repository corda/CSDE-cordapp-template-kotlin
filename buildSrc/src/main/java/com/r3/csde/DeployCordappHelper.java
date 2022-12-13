package com.r3.csde;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import net.corda.v5.base.types.MemberX500Name;
import org.jetbrains.annotations.NotNull;

import javax.naming.ConfigurationException;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.net.HttpURLConnection.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeployCordappHelper {

    public DeployCordappHelper() {
    }
    ProjectContext pc;
    CordaStatusQueries queries;
    ProjectUtils utils;

    public DeployCordappHelper(ProjectContext _pc) {
        pc = _pc;
        queries = new CordaStatusQueries(pc);
        utils = new ProjectUtils(pc);
    }

    public void deployCPIs() throws FileNotFoundException, CsdeException{

        uploadCertificate(pc.signingCertAlias, pc.signingCertFName);
        uploadCertificate(pc.keystoreAlias, pc.keystoreCertFName);

        String appCPILocation = String.format("%s/%s-%s.cpi",
                pc.project.getBuildDir(),
                pc.project.getName(),
                pc.project.getVersion());
        deployCPI(appCPILocation, pc.appCPIName,pc.project.getVersion().toString());

        String notaryCPILocation = String.format("%s/%s-%s.cpi",
                pc.project.getBuildDir(),
                pc.notaryCPIName.replace(' ','-').toLowerCase(),
                pc.project.getVersion());
        deployCPI(notaryCPILocation,
                pc.notaryCPIName,
                pc.project.getVersion().toString(),
                "-NotaryServer" );

    }



    // KV pairs of representative x500 name and corresponding notary service x500 name
    public Map<String, String> getNotaryRepresentatives() throws IOException, ConfigurationException {
        if (pc.notaryRepresentatives == null) {
            pc.notaryRepresentatives = new HashMap<>();

            ObjectMapper mapper = new ObjectMapper();

            FileInputStream in = new FileInputStream(pc.X500ConfigFile);
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);

            List<String> identities = utils.getConfigX500Ids(pc.X500ConfigFile);

            for (com.fasterxml.jackson.databind.JsonNode notary : jsonNode.get("notaries")) {

                String svcX500Id = utils.jsonNodeToString(notary.get("serviceX500Name"));

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

                    String repAsString = utils.jsonNodeToString(representative);

                    if (identities.contains(repAsString)) {
                        pc.notaryRepresentatives.put(repAsString, svcX500Id);
                    } else {
                        throw new ConfigurationException(
                                "Notary representative \"" + repAsString + "\" is not a valid identity");
                    }
                }
            }
        }

        return pc.notaryRepresentatives;
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


    public void reportError(@NotNull kong.unirest.HttpResponse<JsonNode> response) throws CsdeException {

        pc.out.println("*** *** ***");
        pc.out.println("Unexpected response from Corda");
        pc.out.println("Status="+ response.getStatus());
        pc.out.println("*** Headers ***\n"+ response.getHeaders());
        pc.out.println("*** Body ***\n"+ response.getBody());
        pc.out.println("*** *** ***");
        throw new CsdeException("Error: unexpected response from Corda.");
    }

    public void uploadCertificate(String certAlias, String certFName) {
        Unirest.config().verifySsl(false);
        kong.unirest.HttpResponse<JsonNode> uploadResponse = Unirest.put(pc.baseURL + "/api/v1/certificates/cluster/code-signer")
                .field("alias", certAlias)
                .field("certificate", new File(certFName))
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();
        pc.out.println("Certificate/key upload, alias "+certAlias+" certificate/key file "+certFName);
        pc.out.println(uploadResponse.getBody().toPrettyString());
    }

    public void forceuploadCPI(String cpiFName) throws FileNotFoundException, CsdeException {
        forceuploadCPI(cpiFName, "");
    }

    public void forceuploadCPI(String cpiFName, String uploadStatusQualifier) throws FileNotFoundException, CsdeException {
        Unirest.config().verifySsl(false);
        kong.unirest.HttpResponse<JsonNode> jsonResponse = Unirest.post(pc.baseURL + "/api/v1/maintenance/virtualnode/forcecpiupload/")
                .field("upload", new File(cpiFName))
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(jsonResponse.getStatus() == HTTP_OK) {
            String id = (String) jsonResponse.getBody().getObject().get("id");
            pc.out.println("get id:\n" +id);
            kong.unirest.HttpResponse<JsonNode> statusResponse = uploadStatus(id);

            if (statusResponse.getStatus() == HTTP_OK) {
                PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(
                        pc.CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json" )));
                cpiUploadStatus.print(statusResponse.getBody());
                pc.out.println("Caching CPI file upload status:\n" + statusResponse.getBody());
            } else {
                reportError(statusResponse);
            }
        }
        else {
            reportError(jsonResponse);
        }
    }

    private boolean uploadStatusRetry(kong.unirest.HttpResponse<JsonNode> response) {
        int status = response.getStatus();
        JsonNode body = response.getBody();
        // Do not retry on success
        if(status == HTTP_OK) {
            // Keep retrying until we get "OK" may move through "Validating upload", "Persisting CPI"
            return !(body.getObject().get("status").equals("OK"));
        }
        else if (status == HTTP_BAD_REQUEST){
            JSONObject details = response.getBody().getObject().getJSONObject("details");
            if( details != null ){
                String code = (String) details.getString("code");
                return !code.equals("BAD_REQUEST");
            }
            else {
                // Not HTTP_BAD_REQUEST implies some transient problem
                return true;
            }
        }
        return false;
    }

    public kong.unirest.HttpResponse<JsonNode> uploadStatus(String requestId) {
        kong.unirest.HttpResponse<JsonNode> statusResponse = null;
        do {
            utils.rpcWait(1000);
            statusResponse = Unirest
                    .get(pc.baseURL + "/api/v1/cpi/status/" + requestId + "/")
                    .basicAuth(pc.rpcUser, pc.rpcPasswd)
                    .asJson();
            pc.out.println("Upload status="+statusResponse.getStatus()+", status query response:\n"+statusResponse.getBody().toPrettyString());
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

        kong.unirest.HttpResponse<JsonNode> cpiResponse  = queries.getCpiInfo();
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
        pc.out.println("Matching CPIS="+matches);

        if(matches == 0) {
            kong.unirest.HttpResponse<kong.unirest.JsonNode> uploadResponse = Unirest.post(pc.baseURL + "/api/v1/cpi/")
                    .field("upload", new File(cpiFName))
                    .basicAuth(pc.rpcUser, pc.rpcPasswd)
                    .asJson();

            kong.unirest.JsonNode body = uploadResponse.getBody();

            int status = uploadResponse.getStatus();

            pc.out.println("Upload Status:" + status);
            pc.out.println("Pretty print the body\n" + body.toPrettyString());

            // We expect the id field to be a string.
            if (status == HTTP_OK) {
                String id = (String) body.getObject().get("id");
                pc.out.println("get id:\n" + id);

                kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = uploadStatus(id);
                if (statusResponse.getStatus() == HTTP_OK) {
                    PrintStream cpiUploadStatus = new PrintStream(new FileOutputStream(
                            pc.CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json" )));
                    cpiUploadStatus.print(statusResponse.getBody());
                    pc.out.println("Caching CPI file upload status:\n" + statusResponse.getBody());
                } else {
                    reportError(statusResponse);
                }
            } else {
                reportError(uploadResponse);
            }
        }
        else {
            pc.out.println("CPI already uploaded doing a 'force' upload.");
            forceuploadCPI(cpiFName);
        }
    }

    private boolean isMembershipRegComplete(kong.unirest.HttpResponse<kong.unirest.JsonNode> response) throws CsdeException {
        if(response.getStatus() == HTTP_OK) {
            kong.unirest.JsonNode responseBody = response.getBody();
            pc.out.println(responseBody.toPrettyString());
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

    private void pollForCompleteMembershipRegistration(Map<String, String> X500ToShortIdHash) throws CsdeException {
        HashSet<String> vnodesToCheck = new HashSet<String>(X500ToShortIdHash.keySet());
        LinkedList<String> approved = new LinkedList<String>();
        while (!vnodesToCheck.isEmpty()) {
            utils.rpcWait(2000);
            approved.clear();
            for (String vnodeX500 : vnodesToCheck) {
                try {
                    pc.out.println("Checking membership registration progress for v-node '" + vnodeX500 + "':");
                    kong.unirest.HttpResponse<kong.unirest.JsonNode> statusResponse = Unirest
                            .get(pc.baseURL + "/api/v1/membership/" + X500ToShortIdHash.get(vnodeX500) + "/")
                            .basicAuth(pc.rpcUser, pc.rpcPasswd)
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

    public void createAndRegVNodes() throws IOException, CsdeException, ConfigurationException{
        Unirest.config().verifySsl(false);
        String appCpiCheckSum = getLastCPIUploadChkSum( pc.CPIUploadStatusFName );
        String notaryCpiCheckSum = getLastCPIUploadChkSum( pc.CPIUploadStatusFName, "-NotaryServer" );

        LinkedList<String> x500Ids = utils.getConfigX500Ids(pc.X500ConfigFile);
        // Map of X500 name to short hash
        Map<String, String> OKHoldingX500AndShortIds = new HashMap<>();

        // For each identity check that it already exists.
        Set<MemberX500Name> existingX500 = new HashSet<>();
        kong.unirest.HttpResponse<kong.unirest.JsonNode> vnodeListResponse = queries.getVNodeInfo();

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

                pc.out.println("Creating VNode for x500id=\"" + x500id + "\" cpi checksum=" + cpiCheckSum);
                responses.put(x500id, Unirest
                        .post(pc.baseURL + "/api/v1/virtualnode")
                        .body("{ \"request\" : { \"cpiFileChecksum\": " + cpiCheckSum + ", \"x500Name\": \"" + x500id + "\" } }")
                        .basicAuth(pc.rpcUser, pc.rpcPasswd)
                        .asJsonAsync()
                );
            }
            else {
                pc.out.println("Not creating a vnode for \"" + x500id + "\", vnode already exists.");
            }
        }

        pc.out.println("Waiting for VNode creation results...");

        for (Map.Entry<String, CompletableFuture<HttpResponse<JsonNode>>> response: responses.entrySet()) {
            try {
                HttpResponse<JsonNode> jsonNode = response.getValue().get();
                // need to check this and report errors.
                // 200/HTTP_OK - OK
                // 409/HTTP_CONFLICT - Vnode already exists
                if (jsonNode.getStatus() != HTTP_CONFLICT) {
                    if (jsonNode.getStatus() != HTTP_OK) {
                        reportError(jsonNode);
                    } else {
                        JSONObject thing = jsonNode.getBody().getObject().getJSONObject("holdingIdentity");
                        String shortHash = (String) thing.get("shortHash");
                        OKHoldingX500AndShortIds.put(response.getKey(), shortHash);
                    }
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new CsdeException("Unexpected exception while waiting for response to " +
                        "membership submission for holding identity" + response.getKey());
            }
        }

        // Register the VNodes
        responses.clear();

        for(String okId: OKHoldingX500AndShortIds.keySet()) {
            responses.put(okId, Unirest
                    .post(pc.baseURL + "/api/v1/membership/" +  OKHoldingX500AndShortIds.get(okId))
                    .body(getMemberRegistrationBody(okId))
                    .basicAuth(pc.rpcUser, pc.rpcPasswd)
                    .asJsonAsync( response ->
                            pc.out.println("Vnode membership submission for \"" + okId + "\"" +
                                    System.lineSeparator() + response.getBody().toPrettyString()))
            );
        }

        pc.out.println("Vnode membership requests submitted, waiting for acknowledgement from MGM...");

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
