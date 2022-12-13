package com.r3.csde;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;

public class CordaStatusQueries {

    ProjectContext pc;
    public CordaStatusQueries(ProjectContext _pc){ pc = _pc; }

    // X500Name, shorthash, cpiname
//    public void listVNodes() {
//        kong.unirest.HttpResponse<JsonNode> vnodeResponse = getVNodeInfo();
//
//        kong.unirest.json.JSONArray virtualNodesJson = (JSONArray) vnodeResponse.getBody().getObject().get("virtualNodes");
//        pc.out.println("X500 Name\tHolding identity short hash\tCPI Name");
//        for(Object o: virtualNodesJson){
//            if(o instanceof kong.unirest.json.JSONObject) {
//                kong.unirest.json.JSONObject idObj = ((kong.unirest.json.JSONObject) o).getJSONObject("holdingIdentity");
//                kong.unirest.json.JSONObject cpiObj = ((kong.unirest.json.JSONObject) o).getJSONObject("cpiIdentifier");
//                pc.out.print("\"" + idObj.get("x500Name") + "\"");
//                pc.out.print("\t\"" + idObj.get("shortHash") + "\"");
//                pc.out.println("\t\"" + cpiObj.get("cpiName") + "\"");
//            }
//        }
//    }
//
//    public kong.unirest.HttpResponse<JsonNode> getVNodeInfo() {
//        Unirest.config().verifySsl(false);
//        return Unirest.get(pc.baseURL + "/api/v1/virtualnode/")
//                .basicAuth(pc.rpcUser, pc.rpcPasswd)
//                .asJson();
//    }
//
//
//    // not used
//    public void listVNodesVerbose() {
//        kong.unirest.HttpResponse<JsonNode> vnodeResponse = getVNodeInfo();
//        pc.out.println("VNodes:\n" + vnodeResponse.getBody().toPrettyString());
//    }



}
