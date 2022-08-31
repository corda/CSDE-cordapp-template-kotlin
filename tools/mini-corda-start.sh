#!/usr/bin/env bash 


COMBIWORKER_JAR=${1:-$(ls ~/.corda/corda5/corda-comb*.jar | tail -n 1)}

echo "Running: $COMBIWORKER_JAR"


JDBC_JAR_DIR=~/.corda/corda5/jdbcDrivers


# As of 2022-08-04 this know to be the right way - Re convo w/ Lorcan
java \
   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
   -jar $COMBIWORKER_JAR \
   --instanceId=0 -mbus.busType=DATABASE \
   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
   -ddatabase.user=user -ddatabase.pass=password \
   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster \
   -ddatabase.jdbc.directory=$JDBC_JAR_DIR

