#!/usr/bin/env bash -x
CPI=${1:-$(ls *.cpi | tail -n 1)}


read REQUEST_ID < <(echo $(curl --insecure -u admin:admin  -s -F upload=@$CPI https://localhost:8888/api/v1/cpi/  | jq -r '.id') ) 
echo "Uploaded with  id = " $REQUEST_ID

sleep 3
CPI_HASH=$(curl -s --insecure -u admin:admin  https://localhost:8888/api/v1/cpi/status/$REQUEST_ID | jq -r '.checksum')
echo "CPI hash uploaded = $CPI_HASH"

sleep 3

#CPI_HASH="5CC4E88369BD"

echo "Create VNodes for Default network"
 
X500Alice="O=Alice, L=London, C=GB"
HOLDING_IDAlice=$(curl -s --insecure -u admin:admin -s -d '{ "request": { "cpiFileChecksum": "'"$CPI_HASH"'", "x500Name": "'"$X500Alice"'"  } }' https://localhost:8888/api/v1/virtualnode | jq -r '.holdingIdHash')
echo "Holding id = $HOLDING_IDAlice"
sleep 2
#curl -s --insecure -u admin:admin -d '{ "memberRegistrationRequest": { "action": "requestJoin",  "context": { "corda.key.scheme" : "CORDA.ECDSA.SECP256R1" } } }' https://localhost:8888/api/v1/membership/$HOLDING_IDAlice


X500Bob="O=Bob, L=London, C=GB"
HOLDING_IDBob=$(curl -s --insecure -u admin:admin -s -d '{ "request": { "cpiFileChecksum": "'"$CPI_HASH"'", "x500Name": "'"$X500Bob"'"  } }' https://localhost:8888/api/v1/virtualnode | jq -r '.holdingIdHash')
echo "Holding id = $HOLDING_IDBob"
sleep 2
#curl -s --insecure -u admin:admin -d '{ "memberRegistrationRequest": { "action": "requestJoin",  "context": { "corda.key.scheme" : "CORDA.ECDSA.SECP256R1" } } }' https://localhost:8888/api/v1/membership/$HOLDING_IDBob

X500Charlie="O=Charlie, L=London, C=GB" 
HOLDING_IDCharlie=$(curl -s --insecure -u admin:admin -s -d '{ "request": { "cpiFileChecksum": "'"$CPI_HASH"'", "x500Name": "'"$X500Charlie"'"  } }' https://localhost:8888/api/v1/virtualnode | jq -r '.holdingIdHash')
echo "Holding id = $HOLDING_IDCharlie"
sleep 2
#curl -s --insecure -u admin:admin -d '{ "memberRegistrationRequest": { "action": "requestJoin",  "context": { "corda.key.scheme" : "CORDA.ECDSA.SECP256R1" } } }' https://localhost:8888/api/v1/membership/$HOLDING_IDCharlie



echo "Note these holding IDs:"
echo "Alice: $X500Alice"
echo "Bob: $X500Bob"
echo "Charlie: $X500Charlie"
