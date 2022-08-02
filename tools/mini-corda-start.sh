#!/usr/bin/env bash -x

#COMBIWORKER_JAR=$(ls ~/.corda/corda5/working/corda-comb*.jar | tail -n 1)
COMBIWORKER_JAR=${1:-$(ls ~/.corda/corda5/corda-comb*.jar | tail -n 1)}

echo "Running: $COMBIWORKER_JAR"

#java \
#   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
#   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
#   -jar $COMBIWORKER_JAR \
#   --instanceId=0 -mbus.busType=DATABASE \
#   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
#   -ddatabase.user=user -ddatabase.pass=password \
#   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster

java \
   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
   -jar $COMBIWORKER_JAR \
   --instanceId=0 -mbus.busType=DATABASE \
   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
   -ddatabase.user=postgres -ddatabase.pass=password \
   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster


#java \
#   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
#   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
#   -jar $COMBIWORKER_JAR \
#   --instanceId=0 -mbus.busType=DATABASE \
#   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
#   -ddatabase.user=cordappdev -ddatabase.pass=password \
#   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster
