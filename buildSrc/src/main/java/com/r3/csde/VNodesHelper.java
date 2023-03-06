package com.r3.csde;

import java.io.IOException;
import java.util.List;

public class VNodesHelper {
    private ProjectContext pc;
    private ProjectUtils utils;
    private List<String> x500Names;

    public VNodesHelper(ProjectContext _pc) {
        pc = _pc;
        utils = new ProjectUtils(pc);
    }

    public void vNodesSetup() throws IOException {

        pc.out.println(pc.X500ConfigFile);
        NetworkConfig config = new NetworkConfig("config/static-network-config.json");


        List<VNode> nodes = config.getVNodes();

        for (VNode v : nodes) {
            pc.out.println(v.getX500Name());
            pc.out.println(v.getCpis());
            pc.out.println(v.getServiceX500Name());
        }


        // get network config
//        x500Names = utils.getConfigX500Ids(pc.X500ConfigFile);

//        for (String x: x500Names) pc.out.println(x);
//
//        String x500 = x500Names.get(0);
//
//        createVNode(x500);

    }

    private void createVNode(String x500){

        if (checkVNodeExists(x500)){
            pc.out.println("VNode '" + x500 + "' already exists, not recreating");
        } else {

        }

    }

    private boolean checkVNodeExists(String x500){
        return true;
    }

    private void requestVnodeCreationAndPoll() {


    }


}
