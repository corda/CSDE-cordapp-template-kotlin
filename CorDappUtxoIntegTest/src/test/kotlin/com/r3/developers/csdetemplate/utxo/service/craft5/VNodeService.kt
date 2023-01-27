package com.r3.developers.csdetemplate.utxo.service.craft5

import com.r3.developers.csdetemplate.utxo.service.craft5.C5RestService.Companion.getResponse
import com.r3.developers.csdetemplate.utxo.service.craft5.C5RestService.Companion.setupConnection
import net.corda.craft5.http.Http
import net.corda.libs.virtualnode.types.HoldingIdentity
import net.corda.libs.virtualnode.types.VirtualNodeInfo
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import java.net.HttpURLConnection

//TODO: emko: retry -> net.corda.craft5.util.retry ?
//TODO: emko: error handling
//TODO: emko: logging -> net.corda.craft5.logging.CraftLogger ?
class VNodeService {

    companion object {
        var path = "virtualnode"

        /*
        /virtualnode
        This method lists all virtual nodes in the cluster.
         */
        fun listVNodes(http: Http): List<VirtualNodeInfo> {
            setupConnection(http)
            http.get("virtualnode")
            return getResponse<VirtualNodes>(http, HttpURLConnection.HTTP_OK).virtualNodes
        }

        fun listVNodesMap(http: Http): Map<String, HoldingIdentity> {
            return listVNodes(http).associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName!! to it.holdingIdentity }
        }
    }
}
