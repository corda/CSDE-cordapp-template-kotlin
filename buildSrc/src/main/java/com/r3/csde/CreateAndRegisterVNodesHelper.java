package com.r3.csde;

import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import net.corda.v5.base.types.MemberX500Name;
import org.jetbrains.annotations.NotNull;

import javax.naming.ConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_OK;

public class CreateAndRegisterVNodesHelper {

    ProjectContext pc;
    ProjectUtils utils;
    CordaStatusQueries queries;

    public CreateAndRegisterVNodesHelper(ProjectContext _pc) {
        pc = _pc;
        queries = new CordaStatusQueries(pc);
        utils = new ProjectUtils(pc);
    }

    public void createAndRegVNodes() throws IOException, CsdeException, ConfigurationException {
        Unirest.config().verifySsl(false);
        String appCpiCheckSum = getLastCPIUploadChkSum( pc.CPIUploadStatusFName );
        String notaryCpiCheckSum = getLastCPIUploadChkSum( pc.CPIUploadStatusFName, "-NotaryServer" );

        LinkedList<String> x500Ids = utils.getConfigX500Ids(pc.X500ConfigFile);
        // Map of X500 name to short hash
        Map<String, String> OKHoldingX500AndShortIds = new HashMap<>();

        // For each identity check that it already exists.
        Set<MemberX500Name> existingX500 = new HashSet<>();
        HttpResponse<JsonNode> vnodeListResponse = queries.getVNodeInfo();

        JSONArray virtualNodesJson = (JSONArray) vnodeListResponse.getBody().getObject().get("virtualNodes");
        for(Object o: virtualNodesJson){
            if(o instanceof JSONObject) {
                JSONObject idObj = ((JSONObject) o).getJSONObject("holdingIdentity");
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
                        utils.reportError(jsonNode);
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

    public String getLastCPIUploadChkSum(@NotNull String CPIUploadStatusFName) throws IOException, NullPointerException {
        return getLastCPIUploadChkSum(CPIUploadStatusFName, "");
    }

    public String getLastCPIUploadChkSum(@NotNull String CPIUploadStatusFName,
                                                String uploadStatusQualifier) throws IOException, NullPointerException {

        String qualifiedCPIUploadStatusFName =
                CPIUploadStatusFName.replace(".json", uploadStatusQualifier + ".json");

        ObjectMapper mapper = new ObjectMapper();
        FileInputStream in = new FileInputStream(qualifiedCPIUploadStatusFName);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);

        String checksum = jsonNode.get("cpiFileChecksum").toString();
        if(checksum == null || checksum.equals("null")) {
            throw new NullPointerException("Missing cpiFileChecksum in file " +
                    qualifiedCPIUploadStatusFName + " with contents:" + jsonNode);
        }
        return checksum;
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

    private void pollForCompleteMembershipRegistration(Map<String, String> X500ToShortIdHash) throws CsdeException {
        HashSet<String> vnodesToCheck = new HashSet<String>(X500ToShortIdHash.keySet());
        LinkedList<String> approved = new LinkedList<String>();
        while (!vnodesToCheck.isEmpty()) {
            utils.rpcWait(2000);
            approved.clear();
            for (String vnodeX500 : vnodesToCheck) {
                try {
                    pc.out.println("Checking membership registration progress for v-node '" + vnodeX500 + "':");
                    HttpResponse<JsonNode> statusResponse = Unirest
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

    private boolean isMembershipRegComplete(HttpResponse<JsonNode> response) throws CsdeException {
        if(response.getStatus() == HTTP_OK) {
            JsonNode responseBody = response.getBody();
            pc.out.println(responseBody.toPrettyString());
            if(responseBody.getArray().length() > 0) {
                JSONObject memRegStatusInfo = (JSONObject) responseBody
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
            utils.reportError(response);
        }
        return false;
    }


}
