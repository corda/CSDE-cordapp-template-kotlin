#!/usr/bin/env bash


CPB=${1:-$(find . -name "*.cpb" -print)}
echo "CPB=$CPB"
CORDA_CLI_DIR=../all-in-one-worker/corda-cli-plugin-host/ 

# CORDA_CLI=../../../corda-cli-plugin-host/build/generatedScripts/corda-cli.sh
CORDA_CLI=${CORDA_CLI_DIR}/build/generatedScripts/corda-cli.sh
# for win it's corda-cli.cmd
 
CPI=$(basename $CPB .cpb).cpi

if [ ! -f signingkeys.pfx ]; 
then 
  keytool -genkey \
    -alias "my signing key" \
    -keystore signingkeys.pfx \
    -storepass "keystore password" \
    -dname "cn=CPI Example - My Signing Key, o=CorpOrgCorp, c=GB" \
    -keyalg RSA \
    -storetype pkcs12 \
    -validity 4000

  keytool -importcert \
        -alias "freetsa" \
        -keystore signingkeys.pfx \
        -storepass "keystore password" \
        -file cacert.pem \
        -noprompt
fi



rm $CPI




echo "Make fresh MyGroupPolicy.json file"
$CORDA_CLI mgm groupPolicy > MyGroupPolicy.json

echo "Create CPI"
$CORDA_CLI package create \
    --cpb $CPB \
    --group-policy MyGroupPolicy.json \
    --keystore signingkeys.pfx \
    --storepass "keystore password" \
    --key "my signing key" \
    --tsa https://freetsa.org/tsr \
    --file $CPI

