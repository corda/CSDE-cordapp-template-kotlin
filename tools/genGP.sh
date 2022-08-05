#!/usr/bin/env bash

CORDA_CLI_DIR=~/.corda/corda5/corda-cli-plugin-host/

# CORDA_CLI=../../../corda-cli-plugin-host/build/generatedScripts/corda-cli.sh
CORDA_CLI=${CORDA_CLI_DIR}/build/generatedScripts/corda-cli.sh


echo "Make fresh MyGroupPolicy.json file"

$CORDA_CLI mgm groupPolicy --name="C=GB, L=London, O=Doughnut" --name="C=GB, L=London, O=Brownie" --name="C=GB, L=London, O=IceCream" --endpoint-protocol=1 --endpoint="http://localhost:1080"

