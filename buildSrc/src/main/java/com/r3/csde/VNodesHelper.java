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

            if (!checkRegistration(shortHash)){
                registerVnode(vn, shortHash);
            };

        }

    }
    private void registerVnode( VNode vn, String shortHash){

        pc.out.println("Registering vNode: " + vn.getX500Name() + " with shortHash: " + shortHash);

    }


    private boolean checkRegistration(String shortHash) throws JsonProcessingException {

        // queries registration status for vnode
        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/membership/" + shortHash)
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        //
        if(response.getStatus() != HTTP_OK) {
            // todo : add exception
            pc.out.print("Failed to check registration status for " + shortHash);
        }

        // if the response is not empty check all previous requests for an "APPROVED"
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
}
