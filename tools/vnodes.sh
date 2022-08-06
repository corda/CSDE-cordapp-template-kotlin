#!/usr/bin/env bash -x

CPI_HASH=$1


echo "Create VNodes for Default network"
 
X500Alice="C=GB, L=London, O=Alice"
HOLDING_IDAlice=$(curl -s --insecure -u admin:admin -s -d '{ "request": { "cpiFileChecksum": "'"$CPI_HASH"'", "x500Name": "'"$X500Alice"'"  } }' https://localhost:8888/api/v1/virtualnode | jq -r '.shortHash')
echo "Holding id = " $HOLDING_IDAlice
sleep 2
curl -s --insecure -u admin:admin -d '{ "memberRegistrationRequest": { "action": "requestJoin",  "context": { "corda.key.scheme" : "CORDA.ECDSA.SECP256R1" } } }' https://localhost:8888/api/v1/membership/$HOLDING_IDAlice


X500Bob="C=GB, L=London, O=Bob"
HOLDING_IDBob=$(curl -s --insecure -u admin:admin -s -d '{ "request": { "cpiFileChecksum": "'"$CPI_HASH"'", "x500Name": "'"$X500Bob"'"  } }' https://localhost:8888/api/v1/virtualnode | jq -r '.shortHash')
echo "Holding id = " $HOLDING_IDBob
sleep 2
curl -s --insecure -u admin:admin -d '{ "memberRegistrationRequest": { "action": "requestJoin",  "context": { "corda.key.scheme" : "CORDA.ECDSA.SECP256R1" } } }' https://localhost:8888/api/v1/membership/$HOLDING_IDBob

X500Charlie="C=GB, L=London, O=Charlie" 
HOLDING_IDCharlie=$(curl -s --insecure -u admin:admin -s -d '{ "request": { "cpiFileChecksum": "'"$CPI_HASH"'", "x500Name": "'"$X500Charlie"'"  } }' https://localhost:8888/api/v1/virtualnode | jq -r '.shortHash')
echo "Holding id = " $HOLDING_IDCharlie
sleep 2
curl -s --insecure -u admin:admin -d '{ "memberRegistrationRequest": { "action": "requestJoin",  "context": { "corda.key.scheme" : "CORDA.ECDSA.SECP256R1" } } }' https://localhost:8888/api/v1/membership/$HOLDING_IDCharlie



