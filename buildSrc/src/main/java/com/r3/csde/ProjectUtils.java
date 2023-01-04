package com.r3.csde;

import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

public class ProjectUtils {

    ProjectContext pc;
    ProjectUtils(ProjectContext _pc) {
        pc = _pc;
    }


    void rpcWait(int millis) {
        try {
            sleep(millis);
        }
        catch(InterruptedException e) {
            throw new UnsupportedOperationException("Interrupts not supported.", e);
        }
    }

    void rpcWait() {
        rpcWait( pc.retryWaitMs);
    }

    public LinkedList<String> getConfigX500Ids(String configFile) throws IOException {
        LinkedList<String> x500Ids = new LinkedList<>();
//        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ObjectMapper mapper = new ObjectMapper();


        FileInputStream in = new FileInputStream(configFile);
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(in);
        for( com.fasterxml.jackson.databind.JsonNode identity:  jsonNode.get("identities")) {
            x500Ids.add(jsonNodeToString(identity));
        }
        return x500Ids;
    }

    public String jsonNodeToString(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        String jsonString = jsonNode.toString();
        return jsonString.substring(1, jsonString.length()-1);
    }

    public void downloadFile(String url, String targetPath) {
        Unirest.get(url)
                .asFile(targetPath)
                .getBody();
    }

    public void reportError(@NotNull HttpResponse<JsonNode> response) throws CsdeException {

        pc.out.println("*** *** ***");
        pc.out.println("Unexpected response from Corda");
        pc.out.println("Status="+ response.getStatus());
        pc.out.println("*** Headers ***\n"+ response.getHeaders());
        pc.out.println("*** Body ***\n"+ response.getBody());
        pc.out.println("*** *** ***");
        throw new CsdeException("Error: unexpected response from Corda.");
    }
}
