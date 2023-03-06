package com.r3.csde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class NetworkConfig {

    public List<VNode> vNodes;


    public NetworkConfig(String configFile) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        FileInputStream in = new FileInputStream(configFile);
        vNodes = mapper.readValue(in, new TypeReference<List<VNode>>(){});
    }

    List<VNode> getVNodes() { return vNodes; }

}
