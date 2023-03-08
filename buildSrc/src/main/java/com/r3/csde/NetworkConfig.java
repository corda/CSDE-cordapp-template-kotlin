package com.r3.csde;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkConfig {

    private List<VNode> vNodes;
    private String configFilePath;

    public NetworkConfig(String _configFilePath) throws IOException {
        configFilePath = _configFilePath;

        ObjectMapper mapper = new ObjectMapper();
        FileInputStream in = new FileInputStream(configFilePath);
        vNodes = mapper.readValue(in, new TypeReference<List<VNode>>(){});
    }

    String getConfigFilePath() {return configFilePath;}

    List<VNode> getVNodes() { return vNodes; }

    List<String> getX500Names() {
        return vNodes.stream().map(vn -> vn.getX500Name()).collect(Collectors.toList());
    }

}
