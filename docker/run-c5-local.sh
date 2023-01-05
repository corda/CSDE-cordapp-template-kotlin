#!/usr/bin/env sh
java -version
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -DsecurityPolicyAllPermissions=true -Dco.paralleluniverse.fibers.verifyInstrumentation=true -jar ./corda-combined-worker-5.0.0.0-Fox-SNAPSHOT.jar --instanceId=0 -mbus.busType=DATABASE -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt -ddatabase.user=user -ddatabase.pass=password -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster -ddatabase.jdbc.directory=./jdbcDrivers
