package com.r3.csde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r3.csde.dtos.CPIFileStatusDTO;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
//import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import static java.lang.Thread.sleep;

// todo: this class needs refactoring
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

    public void reportError(HttpResponse<JsonNode> response) throws CsdeException {

        pc.out.println("*** *** ***");
        pc.out.println("Unexpected response from Corda");
        pc.out.println("Status="+ response.getStatus());
        pc.out.println("*** Headers ***\n"+ response.getHeaders());
        pc.out.println("*** Body ***\n"+ response.getBody());
        pc.out.println("*** *** ***");
        throw new CsdeException("Error: unexpected response from Corda.");
    }
}
