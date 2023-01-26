package com.r3.developers.csdetemplate.utxo.service.craft5

import net.corda.craft5.http.Headers
import net.corda.craft5.http.Http
import net.corda.craft5.http.util.json
import net.corda.libs.virtualnode.types.HoldingIdentity
import net.corda.libs.virtualnode.types.VirtualNodeInfo
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

//TODO: emko: retry
//TODO: emko: error handling
class VNodeService {

    companion object {
        var url = "https://localhost:8888/api/v1/"
        var path = "virtualnode"

        /*
        /virtualnode
        This method lists all virtual nodes in the cluster.
         */
        fun listVNodes(http: Http): List<VirtualNodeInfo> {
            http.baseUri = URI(url)
            http.baseHeaders = mapOf(
                Headers.basicAuthorization("admin", "admin")
            )

            http.get("virtualnode")
            val response = http.response()
            if (response.status() == HttpURLConnection.HTTP_OK) {
                return response.json<VirtualNodes>().virtualNodes
            }
            throw IOException("${response.uri()} => ${response.body()}")
        }

        fun listVNodesMap(http: Http): Map<String, HoldingIdentity> {
            return listVNodes(http).associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName!! to it.holdingIdentity }
        }
    }
}
