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

    public void vNodesSetup() throws JsonProcessingException {

        pc.out.println(pc.X500ConfigFile);

        List<VNode> requiredNodes = config.getVNodes();
        createVNodes(requiredNodes);

    }

    private void createVNodes(List<VNode> nodes) throws JsonProcessingException {

        // get existing Nodes
        List<VirtualNodeInfoDTO> existingVNodes = getExistingNodes();

        // Check if required Vnodes already exist, if not create them.
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


//                pc.out.println("MB: vnsDTO: " + vnsDTO.getVirtualNodes().get(1).getHoldingIdentity().getX500Name());
//                pc.out.println("MB: vnsDTO: " + vnsDTO.getVirtualNodes().get(0).getCpiIdentifier().getSignerSummaryHash());

// for reference
//            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(response.getBody().toString());
//            VirtualNodesDTO vnsDTO = mapper.treeToValue(jsonNode, VirtualNodesDTO.class);
//            com.fasterxml.jackson.databind.JsonNode vns = jsonNode.get("virtualNodes");

//            for (com.fasterxml.jackson.databind.JsonNode vn : vns){
//                String sh = vn.get("holdingIdentity").get("shortHash").toString();
//                pc.out.println("MB: shorthash: " + sh);
//
//                com.fasterxml.jackson.databind.JsonNode hi = vn.get("holdingIdentity");
//                HoldingIdentityDTO hiDTO = mapper.treeToValue(hi, HoldingIdentityDTO.class);
//                pc.out.println("MB: hiDTO: " + hiDTO.getX500Name());
//
//                VirtualNodeInfoDTO vnDTO = mapper.treeToValue(vn, VirtualNodeInfoDTO.class);
//                pc.out.println("MB: vnDTO: " + vnDTO.getHoldingIdentity().getX500Name());
//            }

            } else {
            // todo: add exception
            pc.out.println("Failed to getExisting Nodes, responseStatus: "+ response.getStatus());
            return Collections.emptyList();
        }
    }

    private void createVNode(VNode vNode) {

        pc.out.println("MB: creating VNode "+ vNode.getX500Name());

    }




    private void requestVnodeCreationAndPoll() {


    }


}
