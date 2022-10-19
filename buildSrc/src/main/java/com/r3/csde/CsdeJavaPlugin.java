package com.r3.csde;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import javax.inject.Inject;
import java.util.Map;


// Version A
/*
public class CsdeJavaPlugin implements Plugin<Project> {
        CsdeJavaPlugin (ObjectFactory) {

        }

        // lateinit var myCsdeExtension: CsdeExtension private set


    @Override
    public void apply(Project project) {
        CsdeExtension myCsdeExtension = project.getExtensions().create("csdeConfig", CsdeExtension.class);
        project.getTasks().create("csdeTask", CsdeTask::class.java) {
            it.dbContainerName.set(myCsdeExtension.dbContainerName)
            it.serverURL.set(myCsdeExtension.serverURL)
        }

        project.tasks.create("whichOS", WhichOSTask::class.java) {
            it.dbContainerName.set(myCsdeExtension.dbContainerName)
            it.serverURL.set(myCsdeExtension.serverURL)
        }

        // project.configurations returns a ConfigurationContainer.
        // val thing: ConfigurationContainer = project.configurations

        project.tasks.create("listCordaLite", ListCordaLite::class.java)
        project.tasks.create("listConfigs", ListConfigs::class.java)


    }
}

 */

// How do I access properties set in gradle.properties?
// How do tasks access a shard CsdeRpcInterface object?
// How do I set dependencies for each task?
// How do I set the group for each task?
// How do I access the project object from a task?

// Version B
public class CsdeJavaPlugin implements Plugin<Project> {
    CsdeRpcInterface csdeHelper;

    /*
    public CsdeJavaPlugin(){

        csdeHelper = new CsdeRpcInterface(project,
                cordaClusterURL.toString(),
                cordaRpcUser,
                cordaRpcPasswd,
                devEnvWorkspace,
                new String("${System.getProperty("java.home")}/bin"),
                dbContainerName,
                cordaJDBCDir,
                combiWorkerPidCacheFile
        );
    }
     */

    private String defaultIfNull(Object in, String defaultVal) {
        return ( in == null ? defaultVal : in.toString() );
    }

    @Override
    public void apply(Project project) {

        //project.getProperties()
        Map<String, ?> projProps = project.getProperties();
        Object foo = project.findProperty("cordaRpcUser");
        String workspaceDir = defaultIfNull(projProps.get("devEnvWorkspace"), "workspace");
        csdeHelper = new CsdeRpcInterface(project,
                defaultIfNull(projProps.get( "cordaClusterURL"), "https://localhost:8888"),
                defaultIfNull(projProps.get("cordaRpcUser"), "admin"),
                defaultIfNull(projProps.get("cordaRpcPasswd"), "admin"),
                workspaceDir,
                System.getProperty("java.home") + "/bin",
                defaultIfNull(projProps.get("dbContainerName"), "CSDEpostgresql"),
                System.getProperty("user.home") + "/.corda/corda5",
                workspaceDir + "/CordaPID.dat"
        );

        project.task("hello").doLast(task-> System.out.println("Hello from CsdeJavaPlugin"));
        //project.task("listVNodes").doLast( task -> { });
        
        //project.getTasks().create("listVNodes", task -> {
        //    task.setGroup("csde");
        //    task.doLast( task1 -> {
        //        csdeHelper.listVNodes();
        //    });
        //});
        project.getTasks().create("newProjInit", NewProjInit.class);
        
        project.getTasks().create("listProjProps", task -> {
                    //Map<String, ?> a = task.getProject().getProperties();
                    Map<String, ?> myMap = task.getProject().getProperties();
                    for(Map.Entry e : myMap.entrySet()){
                        System.out.println(e.getKey() + ":" + e.getValue());
                    }

        }
        );
    }
}