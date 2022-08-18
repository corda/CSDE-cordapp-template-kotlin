#!/usr/bin/env bash -x


#COMBIWORKER_JAR=$(ls ~/.corda/corda5/working/corda-comb*.jar | tail -n 1)
#COMBIWORKER_JAR=${1:-$(ls ~/.corda/corda5/corda-comb*.jar | tail -n 1)}

COMBIWORKER_JAR=/Users/chris.barratt/DevWork/DevExWork/all-in-one-worker/corda-runtime-os/applications/workers/release/combined-worker/build/bin/corda-combined-worker-*.jar

JDBC_JAR_DIR=/Users/chris.barratt/.corda/corda5/jdbcs

echo "Running: $COMBIWORKER_JAR"

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

# As of 2022-08-04 this know to be the wrong way - Re convo w/ Lorcan
#java \
#   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
#   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
#   -jar $COMBIWORKER_JAR \
#   --instanceId=0 -mbus.busType=DATABASE \
#   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
#   -ddatabase.user=postgres -ddatabase.pass=password \
#   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster


#java \
#   '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005' \
#   -Dco.paralleluniverse.fibers.verifyInstrumentation=true \
#   -jar $COMBIWORKER_JAR \
#   --instanceId=0 -mbus.busType=DATABASE \
#   -spassphrase=password -ssalt=salt -spassphrase=password -ssalt=salt \
#   -ddatabase.user=cordappdev -ddatabase.pass=password \
#   -ddatabase.jdbc.url=jdbc:postgresql://localhost:5432/cordacluster

# EXTRA INFO
# To build the combined-worker.
# ./gradlew :applications:workers:release:combined-worker:clean :applications:workers:release:combined-worker:appJar
