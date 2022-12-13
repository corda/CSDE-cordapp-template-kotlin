package com.r3.csde;

import org.gradle.api.Project;

import java.io.PrintStream;
import java.util.Map;

public class ProjectContext {
    Project project;
    String baseURL = "https://localhost:8888";
    String rpcUser = "admin";
    String rpcPasswd = "admin";
     String workspaceDir = "workspace";
    static int retryWaitMs = 1000;
    static PrintStream out = System.out;
    static String CPIUploadStatusBaseName = "CPIFileStatus.json";
    static String CPIUploadStatusFName;
    static String X500ConfigFile = "config/dev-net.json";
    static  String javaBinDir;
    static String cordaPidCache = "CordaPIDCache.dat";
    static String dbContainerName;
    String JDBCDir;
    String combinedWorkerBinRe;
    Map<String, String> notaryRepresentatives = null;
    String signingCertAlias;
    String signingCertFName;
    String keystoreAlias;
    String keystoreFName;
    String keystoreCertFName;
    String appCPIName;
    String notaryCPIName;



    public ProjectContext (Project inProject,
                           String inBaseUrl,
                           String inRpcUser,
                           String inRpcPasswd,
                           String inWorkspaceDir,
                           String inJavaBinDir,
                           String inDbContainerName,
                           String inJDBCDir,
                           String inCordaPidCache,
                           String inSigningCertAlias,
                           String inSigningCertFName,
                           String inKeystoreAlias,
                           String inKeystoreFName,
                           String inKeystoreCertFName,
                           String inAppCPIName,
                           String inNotaryCPIName
    ) {
        project = inProject;
        baseURL = inBaseUrl;
        rpcUser = inRpcUser;
        rpcPasswd = inRpcPasswd;
        workspaceDir = inWorkspaceDir;
        javaBinDir = inJavaBinDir;
        cordaPidCache = inCordaPidCache;
        dbContainerName = inDbContainerName;
        JDBCDir = inJDBCDir;
        CPIUploadStatusFName = workspaceDir + "/" + CPIUploadStatusBaseName;
        signingCertAlias = inSigningCertAlias;
        signingCertFName = inSigningCertFName;
        keystoreAlias = inKeystoreAlias;
        keystoreFName = inKeystoreFName;
        keystoreCertFName = inKeystoreCertFName;
        appCPIName = inAppCPIName;
        notaryCPIName = inNotaryCPIName;
    }
}
