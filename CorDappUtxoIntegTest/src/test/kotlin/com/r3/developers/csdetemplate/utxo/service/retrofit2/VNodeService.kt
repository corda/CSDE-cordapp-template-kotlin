package com.r3.developers.csdetemplate.utxo.service.retrofit2

import net.corda.libs.virtualnode.types.VirtualNodes
import retrofit2.Call
import retrofit2.http.GET

interface VNodeService {

    @GET("virtualnode")
    fun getAll(): Call<VirtualNodes>
}
