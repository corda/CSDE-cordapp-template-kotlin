#!/usr/bin/env bash -x
CPI=${1:-$(ls *.cpi | tail -n 1)}

read REQUEST_ID < <(echo $( curl --insecure -u admin:admin -s -F upload=@$CPI https://localhost:8888/api/v1/maintenance/virtualnode/forcecpiupload/ | jq -r '.id') )
echo "Uploaded with  id = " $REQUEST_ID

sleep 3
CPI_HASH=$(curl -s --insecure -u admin:admin  https://localhost:8888/api/v1/cpi/status/$REQUEST_ID | jq -r '.checksum')
echo "CPI hash uploaded = $CPI_HASH"
