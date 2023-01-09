package com.r3.developers.csdetemplate.utxo

import net.corda.libs.virtualnode.types.VirtualNodes
import retrofit2.Call
import retrofit2.http.GET

interface VNodeService {

    @GET("virtualnode")
    fun getAll(): Call<VirtualNodes>
}
