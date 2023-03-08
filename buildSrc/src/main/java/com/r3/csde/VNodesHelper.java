package com.r3.csde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.csde.dtos.RegistrationRequestProgressDTO;
import com.r3.csde.dtos.VirtualNodeInfoDTO;
import com.r3.csde.dtos.VirtualNodesDTO;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.net.HttpURLConnection.HTTP_OK;

public class VNodesHelper {
    private ProjectContext pc;
    private ProjectUtils utils;
    private NetworkConfig config;
    private ObjectMapper mapper;

    public VNodesHelper(ProjectContext _pc, NetworkConfig _config) {
        pc = _pc;
        utils = new ProjectUtils(pc);
        config = _config;
        Unirest.config().verifySsl(false);
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    // Entry point, called from csde.gradle task
    public void vNodesSetup() throws IOException {

        List<VNode> requiredNodes = config.getVNodes();
        createVNodes(requiredNodes);
        registerVNodes(requiredNodes);

    }

    private void createVNodes(List<VNode> nodes) throws IOException {

        // get existing Nodes
        List<VirtualNodeInfoDTO> existingVNodes = getExistingNodes();

        // Check if each required Vnodes already exist, if not create it.
        for (VNode vn : nodes) {
            List<VirtualNodeInfoDTO> matches = existingVNodes.stream().filter(existing ->
                    existing.getHoldingIdentity().getX500Name().equals( vn.getX500Name()) &&
                    existing.getCpiIdentifier().getCpiName().equals(vn.getCpi()))
                    .collect(Collectors.toList());

            pc.out.println("MB: matches for vn '" + vn.getX500Name() + "': matches: "+ matches);
            if (matches.size() == 0 ) {
                createVNode(vn);
            }
        }
    }

    private List<VirtualNodeInfoDTO> getExistingNodes () throws JsonProcessingException {

        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/virtualnode")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(response.getStatus() == HTTP_OK){
            return mapper.readValue(response.getBody().toString(), VirtualNodesDTO.class).getVirtualNodes();
            } else {
            // todo: add exception
            pc.out.println("Failed to getExisting Nodes, responseStatus: "+ response.getStatus());
            return Collections.emptyList();
        }
    }

    // Creates a Vnode on the corda cluster from the VNode info
    private void createVNode(VNode vNode) throws IOException {

        pc.out.println("MB: creating VNode "+ vNode.getX500Name());

        // read the current CPIFileChecksum value
        String cpiCheckSum = getCpiCheckSum(vNode);
        pc.out.println("MB: checksum: " + cpiCheckSum);

        // creates the vnode on Cluster
        HttpResponse<JsonNode> response = Unirest.post(pc.baseURL + "/api/v1/virtualnode")
                .body("{ \"request\" : { \"cpiFileChecksum\": " + cpiCheckSum + ", \"x500Name\": \"" + vNode.getX500Name() + "\" } }")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        pc.out.println("MB: response: " + response.getStatus());

        // todo: add exceptions if fails
    }


    // Reads the latest CPI checksums from file.
    private String getCpiCheckSum(VNode vNode) throws IOException {

        String file;
        if (vNode.getServiceX500Name() == null){
            file = pc.CPIUploadStatusFName;
        } else {
            file = pc.NotaryCPIUploadStatusFName;
        }
        FileInputStream in = new FileInputStream(file);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);
        return jsonNode.get("cpiFileChecksum").toString();
    }

    // checks if required nodes have been registered and if not registers them
    private void registerVNodes(List<VNode> requiredNodes) throws JsonProcessingException {

        // There appears to be a delay between the successful post /virtualnodes synchronous call and the
        // vnodes being returned in the GET /virtualnodes call. Putting a thread wait here as a quick fix
        // as this will move to async mechanism post beta2.
        utils.rpcWait(2000);
        List<VirtualNodeInfoDTO> existingVNodes = getExistingNodes();

        for (VNode vn: requiredNodes) {
            List<VirtualNodeInfoDTO> matches = existingVNodes.stream().filter(existing ->
                            existing.getHoldingIdentity().getX500Name().equals( vn.getX500Name()) &&
                                    existing.getCpiIdentifier().getCpiName().equals(vn.getCpi()))
                    .collect(Collectors.toList());

            if (matches.size() != 1) {
                // todo: add exception
                pc.out.println("unique look up failed for " + vn.getX500Name());
            }

            String shortHash = matches.get(0).getHoldingIdentity().getShortHash();

            if (!checkVNodeIsRegistered(shortHash)){
                registerVnode(vn, shortHash);
            }
        }
    }
    private void registerVnode( VNode vn, String shortHash) throws JsonProcessingException {
        pc.out.println("Registering vNode: " + vn.getX500Name() + " with shortHash: " + shortHash);

        // Configure the registration body (notary vs non notary)
        String registrationBody;
        if (vn.getServiceX500Name() == null) {
            registrationBody =
                    "{ \"action\" : \"requestJoin\"," +
                            " \"context\" : {" +
                            " \"corda.key.scheme\" : \"CORDA.ECDSA.SECP256R1\" } }";
        } else {
            registrationBody =
                    "{ \"action\" : \"requestJoin\"," +
                            " \"context\" : {" +
                            " \"corda.key.scheme\" : \"CORDA.ECDSA.SECP256R1\", " +
                            " \"corda.roles.0\" : \"notary\", " +
                            " \"corda.notary.service.name\" : \"" + vn.getServiceX500Name() + "\", " +
                            " \"corda.notary.service.plugin\" : \"net.corda.notary.NonValidatingNotary\" } }";
        }

        pc.out.println("MB: registrationBody: " + registrationBody);

        HttpResponse<JsonNode> response = Unirest.post(pc.baseURL + "/api/v1/membership/" + shortHash)
                .body(registrationBody)
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if (response.getStatus() == HTTP_OK) {
            pc.out.println("Membership requested for node " + shortHash);
        } else {
            // todo:add exception
            pc.out.println("Membership request failed for node " + shortHash + " with response code " + response.getStatus() + " and error " + response.getBody());
        }

        // wait until Vnode registered
        pollForRegistration(shortHash, 10000, 1000);

    }


    private boolean checkVNodeIsRegistered(String shortHash) throws JsonProcessingException {

        // queries registration status for vnode
        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/membership/" + shortHash)
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(response.getStatus() != HTTP_OK) {
            // todo : add exception
            pc.out.print("Failed to check registration status for " + shortHash);
        }

        // If the response body is not empty check all previous requests for an "APPROVED"
        if (!response.getBody().getArray().isEmpty()) {
            List<RegistrationRequestProgressDTO> requests = mapper.readValue(
                    response.getBody().toString(), new TypeReference<>() {
                    });
            for (RegistrationRequestProgressDTO request : requests) {
                if (Objects.equals(request.getRegistrationStatus(), "APPROVED")) {
                    return true;
                }
            }
        }
        // Returns false if array was empty or "APPROVED" wasn't found
        return false;
    }

    // polls for registration of a vnode
    private void pollForRegistration(String shortHash, int duration, int cooldown) throws JsonProcessingException {

        int timer = 0;
        while (timer < duration ) {
            if (checkVNodeIsRegistered(shortHash)){
                pc.out.println("Vnode " + shortHash +" registered.");
                return;
            }
            utils.rpcWait(cooldown);
            timer += cooldown;
        }
        pc.out.println("Vnode " + shortHash + " failed to register in " + duration + " milliseconds");
    }
}
