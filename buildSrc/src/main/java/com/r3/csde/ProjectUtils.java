package com.r3.csde;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

public class ProjectUtils {

    static void rpcWait(int millis) {
        try {
            sleep(millis);
        }
        catch(InterruptedException e) {
            throw new UnsupportedOperationException("Interrupts not supported.", e);
        }
    }

    static public LinkedList<String> getConfigX500Ids(String configFile) throws IOException {
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

    static String jsonNodeToString(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        String jsonString = jsonNode.toString();
        return jsonString.substring(1, jsonString.length()-1);
    }

}
