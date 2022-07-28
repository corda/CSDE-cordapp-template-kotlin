#!/usr/bin/env bash 

# COMBIWORKER_JAR=$1
# COMBIWORKER_JAR=$(find . -name "corda-comb*.jar" -print)
COMBIWORKER_JAR=$(find ~/.corda/corda5 "corda-comb*.jar" -print)

# Run postgres docker command here.

java \
   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
   -jar $COMBIWORKER_JAR \
   --instanceId=0 -mbus.busType=DATABASE \
   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
   -ddatabase.user=user -ddatabase.pass=password \
   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster


