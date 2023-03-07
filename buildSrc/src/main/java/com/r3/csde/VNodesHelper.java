package com.r3.csde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.csde.dtos.VirtualNodeInfoDTO;
import com.r3.csde.dtos.VirtualNodesDTO;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.net.HttpURLConnection.HTTP_OK;

public class VNodesHelper {
    private ProjectContext pc;
    private ProjectUtils utils;
    private NetworkConfig config;

    public VNodesHelper(ProjectContext _pc, NetworkConfig _config) {
        pc = _pc;
        utils = new ProjectUtils(pc);
        config = _config;
        Unirest.config().verifySsl(false);

    }

    public void vNodesSetup() throws IOException {

        pc.out.println(pc.X500ConfigFile);

        List<VNode> requiredNodes = config.getVNodes();
        createVNodes(requiredNodes);

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
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        // todo: add exceptions
    }


    // Reads the latest CPI checksums from file.
    private String getCpiCheckSum(VNode vNode) throws IOException {

        String file;
        if (vNode.getServiceX500Name() != null){
            file = pc.CPIUploadStatusFName;
        } else {
            file = pc.NotaryCPIUploadStatusFName;
        }

        ObjectMapper mapper = new ObjectMapper();
        FileInputStream in = new FileInputStream(file);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);
        return jsonNode.get("cpiFileChecksum").toString();
    }


}
