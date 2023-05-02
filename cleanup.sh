#!/bin/bash
jps | grep corda-combined-worker | awk -F ' ' '{print $1}'| xargs kill -9
docker stop CSDEpostgresql
./gradlew clean
rm -rf ~/.corda/corda5
rm -rf workspace
