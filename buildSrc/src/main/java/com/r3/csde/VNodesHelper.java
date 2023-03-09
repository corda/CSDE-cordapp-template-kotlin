package com.r3.csde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.csde.dtos.*;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.net.HttpURLConnection.HTTP_OK;

// The VNodesHelper class is used to create and register the Vnodes specified in the static-network-config.json file.

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

    // Entry point for setting up vnodes, called from the 5-vNodeSetup task in csde.gradle
    public void vNodesSetup() throws IOException, CsdeException {
        List<VNode> requiredNodes = config.getVNodes();
        createVNodes(requiredNodes);
        registerVNodes(requiredNodes);
    }

    // Creates vnodes specified in the config if they don't already exist.
    private void createVNodes(List<VNode> nodes) throws IOException, CsdeException {

        // Get existing Nodes.
        List<VirtualNodeInfoDTO> existingVNodes = getExistingNodes();

        // Check if each required vnode already exist, if not create it.
        for (VNode vn : nodes) {
            // Match on x500 and cpi name
            List<VirtualNodeInfoDTO> matches = existingVNodes.stream().filter(existing ->
                    existing.getHoldingIdentity().getX500Name().equals( vn.getX500Name()) &&
                    existing.getCpiIdentifier().getCpiName().equals(vn.getCpi()))
                    .collect(Collectors.toList());

            if (matches.size() == 0 ) {
                createVNode(vn);
            }
        }
    }

    private List<VirtualNodeInfoDTO> getExistingNodes () throws CsdeException {

        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/virtualnode")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(response.getStatus() != HTTP_OK){
            throw new CsdeException("Failed to get Existing vNodes, response status: "+ response.getStatus());
        }

        try {
            return mapper.readValue(response.getBody().toString(), VirtualNodesDTO.class).getVirtualNodes();
        } catch (Exception e){
            throw new CsdeException("Failed to get Existing vNodes with exception: " + e);
        }
    }

    // Creates a vnode on the corda cluster from the VNode info.
    private void createVNode(VNode vNode) throws CsdeException {

        pc.out.println("Creating virtual node for "+ vNode.getX500Name());

        // Reads the current CPIFileChecksum value and checks it has been uploaded.
        String cpiCheckSum = getCpiCheckSum(vNode);
        if (!checkCpiUploaded(cpiCheckSum)) throw new CsdeException("Cpi " + cpiCheckSum + " not uploaded.");

        // Creates the vnode on Cluster
        HttpResponse<JsonNode> response = Unirest.post(pc.baseURL + "/api/v1/virtualnode")
                .body("{ \"request\" : { \"cpiFileChecksum\": \"" + cpiCheckSum + "\", \"x500Name\": \"" + vNode.getX500Name() + "\" } }")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if (response.getStatus() != HTTP_OK)
            throw new CsdeException("Creation of virtual node failed with response status: " + response.getStatus());
    }

    // Reads the latest CPI checksums from file.
    private String getCpiCheckSum(VNode vNode) throws CsdeException {

        try {
            String file;
            if (vNode.getServiceX500Name() == null) {
                file = pc.CPIUploadStatusFName;
            } else {
                file = pc.NotaryCPIUploadStatusFName;
            }
            FileInputStream in = new FileInputStream(file);
            CPIFileStatusDTO statusDTO = mapper.readValue(in, CPIFileStatusDTO.class);
            return statusDTO.getCpiFileChecksum().toString();
        } catch (Exception e){
            throw new CsdeException("Failed to read CPI checksum from file, with error: " + e);
        }
    }

    private boolean checkCpiUploaded(String cpiCheckSum) throws CsdeException {

        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/cpi")
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(response.getStatus() != HTTP_OK)
            throw new CsdeException("Failed to check cpis, response status: " + response.getStatus());

        try {
            GetCPIsResponseDTO cpisResponse = mapper.readValue(
                    response.getBody().toString(), GetCPIsResponseDTO.class);

            for (CpiMetadataDTO cpi: cpisResponse.getCpis()){
                if (Objects.equals(cpi.getCpiFileChecksum(), cpiCheckSum)){
                    return true;
                }
            }
            // Returns false if no cpis were returned or the cpiCheckSum didnt' match ay results.
            return false;
        } catch (Exception e) {
            throw new CsdeException("Failed to check cpis with exception: " + e);
        }
    }


    // Checks if required vnodes have been registered and if not registers them.
    private void registerVNodes(List<VNode> requiredNodes) throws JsonProcessingException, CsdeException {

        // There appears to be a delay between the successful post /virtualnodes synchronous call and the
        // vnodes being returned in the GET /virtualnodes call. Putting a thread wait here as a quick fix
        // as this will move to async mechanism post beta2.
        utils.rpcWait(2000);
        List<VirtualNodeInfoDTO> existingVNodes = getExistingNodes();

        for (VNode vn: requiredNodes) {
            // Match on x500 and cpi name
            List<VirtualNodeInfoDTO> matches = existingVNodes.stream().filter(existing ->
                            existing.getHoldingIdentity().getX500Name().equals( vn.getX500Name()) &&
                                    existing.getCpiIdentifier().getCpiName().equals(vn.getCpi()))
                    .collect(Collectors.toList());

            if (matches.size() == 0) {
                throw new CsdeException("Registration failed because virtual node for '" + vn.getX500Name() + "' not found.");
            } else if (matches.size() >1 ) {
                throw new CsdeException(("Registration failed because more than virtual node for '" + vn.getX500Name() + "'"));
            }

            String shortHash = matches.get(0).getHoldingIdentity().getShortHash();

            if (!checkVNodeIsRegistered(shortHash)){
                registerVnode(vn, shortHash);
            }
        }
    }
    private void registerVnode( VNode vn, String shortHash) throws CsdeException {

        pc.out.println("Registering vNode: " + vn.getX500Name() + " with shortHash: " + shortHash);

        // Configure the registration body (notary vs non notary)
        String registrationBody;
        if (vn.getServiceX500Name() == null) {
            registrationBody =
                    "{ \"action\" : \"requestJoin\"," +
                            " \"context\" : {" +
                            " \"corda.key.scheme\" : \"CORDA.ECDSA.SECP256R1\" } }";
        } else {
            registrationBody =
                    "{ \"action\" : \"requestJoin\"," +
                            " \"context\" : {" +
                            " \"corda.key.scheme\" : \"CORDA.ECDSA.SECP256R1\", " +
                            " \"corda.roles.0\" : \"notary\", " +
                            " \"corda.notary.service.name\" : \"" + vn.getServiceX500Name() + "\", " +
                            " \"corda.notary.service.plugin\" : \"net.corda.notary.NonValidatingNotary\" } }";
        }

        HttpResponse<JsonNode> response = Unirest.post(pc.baseURL + "/api/v1/membership/" + shortHash)
                .body(registrationBody)
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if (response.getStatus() == HTTP_OK) {
            pc.out.println("Membership requested for node " + shortHash);
        } else {
            throw new CsdeException("Failed to register virtual node "+ shortHash +
                    ", response status: " + response.getStatus() );
            }

        // wait until Vnode registered
        pollForRegistration(shortHash, 30000, 1000);

    }

    // Checks if a virtual node with given shortHash has been registered
    private boolean checkVNodeIsRegistered(String shortHash) throws CsdeException {

        // Queries registration status for vnode.
        HttpResponse<JsonNode> response = Unirest.get(pc.baseURL + "/api/v1/membership/" + shortHash)
                .basicAuth(pc.rpcUser, pc.rpcPasswd)
                .asJson();

        if(response.getStatus() != HTTP_OK)
            throw new CsdeException("Failed to check registration status for virtual node '" + shortHash +
                    "' response status: " + response.getStatus());

        try {
        // If the response body is not empty check all previous requests for an "APPROVED"
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

        } catch (Exception e){
            throw new CsdeException("Failed to check registration status for " + shortHash +
                    " with exception " + e);
        }
    }

    // polls for registration of a vnode
    private void pollForRegistration(String shortHash, int duration, int cooldown) throws CsdeException {

        int timer = 0;
        while (timer < duration ) {
            if (checkVNodeIsRegistered(shortHash)){
                pc.out.println("Vnode " + shortHash +" registered.");
                return;
            }
            utils.rpcWait(cooldown);
            timer += cooldown;
        }
        throw new CsdeException("Vnode " + shortHash + " failed to register in " + duration + " milliseconds");
    }
}
