#!/usr/bin/env bash 

COMBIWORKER_JAR=$1

echo "cordaApiVersion="$(jar tf $COMBIWORKER_JAR | grep corda-application-[0-9].*.jar | sed -e 's/bundles\/corda-application-\(.*\)\.jar/\1/')



