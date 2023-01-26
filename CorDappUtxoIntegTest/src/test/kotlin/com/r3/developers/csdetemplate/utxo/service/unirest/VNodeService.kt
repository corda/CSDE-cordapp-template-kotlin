package com.r3.developers.csdetemplate.utxo.service.unirest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kong.unirest.Unirest
import net.corda.libs.virtualnode.types.HoldingIdentity
import net.corda.libs.virtualnode.types.VirtualNodeInfo
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import java.io.IOException

//TODO: emko: retry
//TODO: emko: error handling
class VNodeService {

    companion object {
        var path = "virtualnode"

        var mapper: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        init {
            //TODO: emko: parametrize:
            Unirest.config()
                .verifySsl(false)
                .setDefaultBasicAuth("admin", "admin")
                .defaultBaseUrl("https://localhost:8888/api/v1/")
            //TODO: emko: look into:
//                .objectMapper = JacksonObjectMapper(mapper)
        }

        /*
        /virtualnode
        This method lists all virtual nodes in the cluster.
         */
        fun listVNodes(): List<VirtualNodeInfo> {
            val request = Unirest.get(path)
            val response = request.asString()
            if (response.isSuccess) {
                return mapper.readValue(response.body, VirtualNodes::class.java).virtualNodes
            }
            throw IOException("${request.url} => ${response.body}")
        }

        fun listVNodesMap(): Map<String, HoldingIdentity> {
            return listVNodes().associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName!! to it.holdingIdentity }
        }
    }
}
