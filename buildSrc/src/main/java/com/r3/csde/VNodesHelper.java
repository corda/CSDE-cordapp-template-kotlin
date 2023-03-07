package com.r3.csde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.csde.dtos.HoldingIdentityDTO;
import com.r3.csde.dtos.VirtualNodeInfoDTO;
import com.r3.csde.dtos.VirtualNodesDTO;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.util.List;

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

        List<VNode> nodes = config.getVNodes();

        for (VNode v : nodes) {
            pc.out.println(v.getX500Name());
            pc.out.println(v.getCpis());
            pc.out.println(v.getServiceX500Name());
        }

        VNode vNode = config.vNodes.get(0);
        createVNodes(nodes);

    }

    private void createVNodes(List<VNode> nodes) throws JsonProcessingException {

        // get existing Nodes
         getExistingNodes();


//        if (checkVNodeExists(vNode)){
//            pc.out.println("VNode '" + vNode.getX500Name() + "' already exists, not recreating");
//        } else {
//            pc.out.println("Creating VNode '" + vNode.getX500Name() + "'.");
//        }

    }

//    private boolean checkVNodeExists(VNode vNode){
//
//        CompletableFuture<HttpResponse<JsonNode>> response =
//                Unirest.get(pc.baseURL + "/api/v1/virtualnode")
//                .basicAuth(pc.rpcUser, pc.rpcPasswd)
//                .asJsonAsync();
//
//
//
//    }


    private void getExistingNodes () throws JsonProcessingException {

        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/virtualnode")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(response.getStatus() == HTTP_OK){

            String body = response.getBody().toString();

            pc.out.println("MB: body: " + body);

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(body);
            pc.out.println("jsonNode: " + jsonNode);

            com.fasterxml.jackson.databind.JsonNode vns = jsonNode.get("virtualNodes");



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

            VirtualNodesDTO vnsDTO = mapper.treeToValue(jsonNode, VirtualNodesDTO.class);
            int noVns = vnsDTO.getVirtualNodes().size();
            if(noVns > 0) {

                pc.out.println("MB: vnsDTO: " + vnsDTO.getVirtualNodes().get(1).getHoldingIdentity().getX500Name());
                pc.out.println("MB: vnsDTO: " + vnsDTO.getVirtualNodes().get(0).getCpiIdentifier().getSignerSummaryHash());

            } else {
                pc.out.println(":MB: no vns");
            }



            } else {

            pc.out.println("Failed to getExisting Nodes");
        }





    }






    private void requestVnodeCreationAndPoll() {


    }


}
